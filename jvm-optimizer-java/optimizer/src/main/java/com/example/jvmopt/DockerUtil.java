package com.example.jvmopt;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/** Вспомогательный класс для запуска Docker контейнера и считывания вывода */
public class DockerUtil {

    /**
     * Запускает контейнер jvm-test-image с переменной окружения JAVA_OPTS,
     * возвращает stdout (в виде строк).
     */
    public static List<String> runContainer(String imageName, String javaOpts, String arg) throws Exception {
        List<String> cmd = new ArrayList<>();
        cmd.add("docker");
        cmd.add("run");
        cmd.add("--rm");
        if (javaOpts != null && !javaOpts.isEmpty()) {
            // передаём JAVA_OPTS как единый аргумент
            cmd.add("-e");
            cmd.add("JAVA_OPTS=" + javaOpts);
        }
        cmd.add(imageName);
        if (arg != null && !arg.isEmpty()) cmd.add(arg);

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);
        Process p = pb.start();

        List<String> output = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
            String line;
            while ((line = br.readLine()) != null) {
                output.add(line);
            }
        }

        int exitCode = p.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("docker run exited with code " + exitCode);
        }
        return output;
    }
}
