package com.accessibilitymanager.root;

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

public final class RootServiceManager {
    private static final String TAG = "RootServiceManager";
    private static final long COMMAND_TIMEOUT_SECONDS = 20;

    public Result setEnabled(String componentName, boolean enabled) {
        final String command;
        try {
            command = ShellCommandBuilder.forService(componentName, enabled);
        } catch (IllegalArgumentException exception) {
            return Result.failure(Error.INVALID_COMPONENT);
        }

        Process process = null;
        try {
            process = new ProcessBuilder("su", "-c", command)
                    .redirectErrorStream(true)
                    .start();
            boolean finished = process.waitFor(COMMAND_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return Result.failure(Error.TIMEOUT);
            }

            String output = readOutput(process);
            int exitCode = process.exitValue();
            if (exitCode == 0) {
                return Result.success();
            }
            if (output.contains("__A11YCTL_MISSING__")) {
                return Result.failure(Error.MODULE_MISSING);
            }
            Log.w(TAG, "Root command failed with " + exitCode + ": " + output);
            return Result.failure(Error.ROOT_DENIED_OR_COMMAND_FAILED);
        } catch (IOException exception) {
            Log.w(TAG, "Unable to start root command", exception);
            return Result.failure(Error.ROOT_UNAVAILABLE);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return Result.failure(Error.INTERRUPTED);
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

    public enum Error {
        NONE,
        INVALID_COMPONENT,
        ROOT_UNAVAILABLE,
        ROOT_DENIED_OR_COMMAND_FAILED,
        MODULE_MISSING,
        TIMEOUT,
        INTERRUPTED
    }

    public static final class Result {
        private final boolean successful;
        private final Error error;

        private Result(boolean successful, Error error) {
            this.successful = successful;
            this.error = error;
        }

        public static Result success() {
            return new Result(true, Error.NONE);
        }

        public static Result failure(Error error) {
            return new Result(false, error);
        }

        public boolean isSuccessful() {
            return successful;
        }

        public Error getError() {
            return error;
        }
    }
}
