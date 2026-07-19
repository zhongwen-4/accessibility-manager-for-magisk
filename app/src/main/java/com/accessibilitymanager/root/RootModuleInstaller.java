package com.accessibilitymanager.root;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

public final class RootModuleInstaller {
    private static final String TAG = "RootModuleInstaller";
    private static final String MODULE_ASSET = "accessibility-manager-module.zip";
    private static final long STATUS_TIMEOUT_SECONDS = 60;
    private static final long INSTALL_TIMEOUT_SECONDS = 90;
    private static final long REBOOT_TIMEOUT_SECONDS = 30;

    private static final String STATUS_COMMAND =
            "read_version() { "
                    + "[ -r \"$1\" ] || { echo 0; return; }; "
                    + "value=$(sed -n 's/^versionCode=//p' \"$1\" | head -n 1); "
                    + "case \"$value\" in ''|*[!0-9]*) value=0;; esac; echo \"$value\"; "
                    + "}; "
                    + "installed=$(read_version /data/adb/modules/accessibility_manager/module.prop); "
                    + "pending=$(read_version /data/adb/modules_update/accessibility_manager/module.prop); "
                    + "command -v a11yctl >/dev/null 2>&1 && mounted=1 || mounted=0; "
                    + "[ -e /data/adb/modules/accessibility_manager/disable ] && disabled=1 || disabled=0; "
                    + "printf '__A11Y_STATUS__:%s:%s:%s:%s\\n' "
                    + "\"$installed\" \"$pending\" \"$mounted\" \"$disabled\"";

    private final Context context;

    public RootModuleInstaller(Context context) {
        this.context = context.getApplicationContext();
    }

    public Result ensureInstalled(int bundledVersionCode) {
        CommandResult inspection = runRootCommand(STATUS_COMMAND, STATUS_TIMEOUT_SECONDS);
        if (!inspection.started) {
            return Result.failure(State.ROOT_UNAVAILABLE);
        }
        if (inspection.timedOut) {
            return Result.failure(State.TIMEOUT);
        }
        if (inspection.exitCode != 0) {
            return Result.failure(State.ROOT_DENIED_OR_COMMAND_FAILED);
        }

        final ModuleStatusParser.Status status;
        try {
            status = ModuleStatusParser.parse(inspection.output);
        } catch (IllegalArgumentException exception) {
            Log.w(TAG, "Unable to parse module status: " + inspection.output, exception);
            return Result.failure(State.ROOT_DENIED_OR_COMMAND_FAILED);
        }

        if (status.getInstalledVersionCode() > 0 && status.isDisabled()) {
            return Result.failure(State.MODULE_DISABLED);
        }
        if (status.getPendingVersionCode() >= bundledVersionCode) {
            return Result.success(State.REBOOT_REQUIRED);
        }
        if (status.getInstalledVersionCode() >= bundledVersionCode) {
            if (status.isMounted()) {
                return Result.success(State.READY);
            }
            return Result.success(State.REBOOT_REQUIRED);
        }

        final File moduleZip;
        try {
            moduleZip = copyBundledModule(bundledVersionCode);
        } catch (IOException exception) {
            Log.w(TAG, "Unable to copy bundled module", exception);
            return Result.failure(State.ASSET_ERROR);
        }

        String installCommand = ModuleInstallCommandBuilder.build(moduleZip.getAbsolutePath());
        CommandResult installation = runRootCommand(installCommand, INSTALL_TIMEOUT_SECONDS);
        if (!installation.started) {
            return Result.failure(State.ROOT_UNAVAILABLE);
        }
        if (installation.timedOut) {
            return Result.failure(State.TIMEOUT);
        }
        if (installation.output.contains(ModuleInstallCommandBuilder.ROOT_MANAGER_MISSING_MARKER)) {
            return Result.failure(State.ROOT_MANAGER_MISSING);
        }
        if (installation.exitCode != 0) {
            Log.w(TAG, "Module installation failed: " + installation.output);
            return Result.failure(State.INSTALL_FAILED);
        }
        return Result.success(State.REBOOT_REQUIRED);
    }

    public Result reboot() {
        CommandResult result = runRootCommand("reboot", REBOOT_TIMEOUT_SECONDS);
        if (!result.started) {
            return Result.failure(State.ROOT_UNAVAILABLE);
        }
        if (result.exitCode == 0) {
            return Result.success(State.REBOOT_REQUIRED);
        }
        if (result.timedOut) {
            return Result.failure(State.TIMEOUT);
        }
        return Result.failure(State.ROOT_DENIED_OR_COMMAND_FAILED);
    }

    private File copyBundledModule(int versionCode) throws IOException {
        File output = new File(
                context.getCacheDir(),
                "accessibility-manager-module-" + versionCode + ".zip"
        );
        try (InputStream input = context.getAssets().open(MODULE_ASSET);
             FileOutputStream outputStream = new FileOutputStream(output, false)) {
            byte[] buffer = new byte[16 * 1024];
            int count;
            while ((count = input.read(buffer)) != -1) {
                outputStream.write(buffer, 0, count);
            }
            outputStream.getFD().sync();
        }
        return output;
    }

    private static CommandResult runRootCommand(String command, long timeoutSeconds) {
        Process process = null;
        try {
            process = new ProcessBuilder("su", "-c", command)
                    .redirectErrorStream(true)
                    .start();
            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return CommandResult.timedOut();
            }
            return CommandResult.finished(process.exitValue(), readOutput(process));
        } catch (IOException exception) {
            Log.w(TAG, "Unable to run root command", exception);
            return CommandResult.notStarted();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return CommandResult.finished(130, "");
        } finally {
            if (process != null) {
                process.destroy();
            }
        }
    }

    private static String readOutput(Process process) throws IOException {
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                process.getInputStream(),
                StandardCharsets.UTF_8
        ))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (output.length() > 0) {
                    output.append('\n');
                }
                output.append(line);
            }
        }
        return output.toString();
    }

    public enum State {
        READY,
        REBOOT_REQUIRED,
        MODULE_DISABLED,
        ROOT_UNAVAILABLE,
        ROOT_DENIED_OR_COMMAND_FAILED,
        ROOT_MANAGER_MISSING,
        ASSET_ERROR,
        INSTALL_FAILED,
        TIMEOUT
    }

    public static final class Result {
        private final boolean successful;
        private final State state;

        private Result(boolean successful, State state) {
            this.successful = successful;
            this.state = state;
        }

        public static Result success(State state) {
            return new Result(true, state);
        }

        public static Result failure(State state) {
            return new Result(false, state);
        }

        public boolean isSuccessful() {
            return successful;
        }

        public State getState() {
            return state;
        }
    }

    private static final class CommandResult {
        private final boolean started;
        private final boolean timedOut;
        private final int exitCode;
        private final String output;

        private CommandResult(boolean started, boolean timedOut, int exitCode, String output) {
            this.started = started;
            this.timedOut = timedOut;
            this.exitCode = exitCode;
            this.output = output;
        }

        private static CommandResult notStarted() {
            return new CommandResult(false, false, -1, "");
        }

        private static CommandResult timedOut() {
            return new CommandResult(true, true, -1, "");
        }

        private static CommandResult finished(int exitCode, String output) {
            return new CommandResult(true, false, exitCode, output);
        }
    }
}
