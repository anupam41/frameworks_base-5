/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.server.intelligence;

import static com.android.server.intelligence.IntelligenceManagerService.RECEIVER_BUNDLE_EXTRA_SESSIONS;

import android.annotation.NonNull;
import android.os.Bundle;
import android.os.ShellCommand;
import android.os.UserHandle;

import com.android.internal.os.IResultReceiver;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Shell Command implementation for {@link IntelligenceManagerService}.
 */
//TODO(b/111276913): rename once the final name is defined
public final class IntelligenceServiceShellCommand extends ShellCommand {

    private final IntelligenceManagerService mService;

    public IntelligenceServiceShellCommand(@NonNull IntelligenceManagerService service) {
        mService = service;
    }

    @Override
    public int onCommand(String cmd) {
        if (cmd == null) {
            return handleDefaultCommands(cmd);
        }
        final PrintWriter pw = getOutPrintWriter();
        switch (cmd) {
            case "list":
                return requestList(pw);
            case "destroy":
                return requestDestroy(pw);
            case "get":
                return requestGet(pw);
            case "set":
                return requestSet(pw);
            default:
                return handleDefaultCommands(cmd);
        }
    }

    @Override
    public void onHelp() {
        try (PrintWriter pw = getOutPrintWriter();) {
            // TODO(b/111276913): rename "intelligence" once SELinux rule changed
            pw.println("Intelligence Service (intelligence) commands:");
            pw.println("  help");
            pw.println("    Prints this help text.");
            pw.println("");
            pw.println("  get bind-instant-service-allowed");
            pw.println("    Gets whether binding to services provided by instant apps is allowed");
            pw.println("");
            pw.println("  set bind-instant-service-allowed [true | false]");
            pw.println("    Sets whether binding to services provided by instant apps is allowed");
            pw.println("");
            pw.println("  list sessions [--user USER_ID]");
            pw.println("    Lists all pending sessions.");
            pw.println("");
            pw.println("  destroy sessions [--user USER_ID]");
            pw.println("    Destroys all pending sessions.");
            pw.println("");
        }
    }

    private int requestGet(PrintWriter pw) {
        final String what = getNextArgRequired();
        switch(what) {
            case "bind-instant-service-allowed":
                return getBindInstantService(pw);
            default:
                pw.println("Invalid set: " + what);
                return -1;
        }
    }

    private int requestSet(PrintWriter pw) {
        final String what = getNextArgRequired();

        switch(what) {
            case "bind-instant-service-allowed":
                return setBindInstantService(pw);
            default:
                pw.println("Invalid set: " + what);
                return -1;
        }
    }

    private int getBindInstantService(PrintWriter pw) {
        if (mService.getAllowInstantService()) {
            pw.println("true");
        } else {
            pw.println("false");
        }
        return 0;
    }

    private int setBindInstantService(PrintWriter pw) {
        final String mode = getNextArgRequired();
        switch (mode.toLowerCase()) {
            case "true":
                mService.setAllowInstantService(true);
                return 0;
            case "false":
                mService.setAllowInstantService(false);
                return 0;
            default:
                pw.println("Invalid mode: " + mode);
                return -1;
        }
    }

    private int requestDestroy(PrintWriter pw) {
        if (!isNextArgSessions(pw)) {
            return -1;
        }

        final int userId = getUserIdFromArgsOrAllUsers();
        final CountDownLatch latch = new CountDownLatch(1);
        final IResultReceiver receiver = new IResultReceiver.Stub() {
            @Override
            public void send(int resultCode, Bundle resultData) {
                latch.countDown();
            }
        };
        return requestSessionCommon(pw, latch, () -> mService.destroySessions(userId, receiver));
    }

    private int requestList(PrintWriter pw) {
        if (!isNextArgSessions(pw)) {
            return -1;
        }

        final int userId = getUserIdFromArgsOrAllUsers();
        final CountDownLatch latch = new CountDownLatch(1);
        final IResultReceiver receiver = new IResultReceiver.Stub() {
            @Override
            public void send(int resultCode, Bundle resultData) {
                final ArrayList<String> sessions = resultData
                        .getStringArrayList(RECEIVER_BUNDLE_EXTRA_SESSIONS);
                for (String session : sessions) {
                    pw.println(session);
                }
                latch.countDown();
            }
        };
        return requestSessionCommon(pw, latch, () -> mService.listSessions(userId, receiver));
    }

    private boolean isNextArgSessions(PrintWriter pw) {
        final String type = getNextArgRequired();
        if (!type.equals("sessions")) {
            pw.println("Error: invalid list type");
            return false;
        }
        return true;
    }

    private int requestSessionCommon(PrintWriter pw, CountDownLatch latch,
            Runnable command) {
        command.run();
        return waitForLatch(pw, latch);
    }

    private int waitForLatch(PrintWriter pw, CountDownLatch latch) {
        try {
            final boolean received = latch.await(5, TimeUnit.SECONDS);
            if (!received) {
                pw.println("Timed out after 5 seconds");
                return -1;
            }
        } catch (InterruptedException e) {
            pw.println("System call interrupted");
            Thread.currentThread().interrupt();
            return -1;
        }
        return 0;
    }

    private int getUserIdFromArgsOrAllUsers() {
        if ("--user".equals(getNextArg())) {
            return UserHandle.parseUserArg(getNextArgRequired());
        }
        return UserHandle.USER_ALL;
    }
}
