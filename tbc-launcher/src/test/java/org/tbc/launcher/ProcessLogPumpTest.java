package org.tbc.launcher;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProcessLogPumpTest {
    @TempDir
    Path tmp;

    @Test
    void pumpWhenProcessStdoutHasLinesShouldTeeToFileAndConsumer() throws Exception {
        Path log = tmp.resolve("logs").resolve("auth.log");
        List<String> seen = new ArrayList<>();
        Process p = new Process() {
            @Override
            public OutputStream getOutputStream() {
                return new ByteArrayOutputStream();
            }

            @Override
            public InputStream getInputStream() {
                return new ByteArrayInputStream("hello\nworld\n".getBytes(StandardCharsets.UTF_8));
            }

            @Override
            public InputStream getErrorStream() {
                return new ByteArrayInputStream(new byte[0]);
            }

            @Override
            public int waitFor() {
                return 0;
            }

            @Override
            public int exitValue() {
                return 0;
            }

            @Override
            public void destroy() {
            }
        };
        Thread t = ProcessLogPump.start("auth", p, log, seen::add);
        t.join(TimeUnit.SECONDS.toMillis(2));
        assertTrue(!t.isAlive());
        assertEquals(List.of("hello", "world"), seen);
        assertEquals("hello\nworld\n", Files.readString(log));
    }

    @Test
    void pumpWhenListenerNullShouldStillWriteLog() throws Exception {
        Path log = tmp.resolve("world.log");
        Process p = new Process() {
            @Override
            public OutputStream getOutputStream() {
                return new ByteArrayOutputStream();
            }

            @Override
            public InputStream getInputStream() {
                return new ByteArrayInputStream("only\n".getBytes(StandardCharsets.UTF_8));
            }

            @Override
            public InputStream getErrorStream() {
                return new ByteArrayInputStream(new byte[0]);
            }

            @Override
            public int waitFor() {
                return 0;
            }

            @Override
            public int exitValue() {
                return 0;
            }

            @Override
            public void destroy() {
            }
        };
        Thread t = ProcessLogPump.start("world", p, log, null);
        t.join(TimeUnit.SECONDS.toMillis(2));
        assertTrue(!t.isAlive());
        assertEquals("only\n", Files.readString(log));
    }
}
