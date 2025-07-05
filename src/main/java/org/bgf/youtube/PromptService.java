package org.bgf.youtube;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class PromptService {
    private static final Scanner scanner = new Scanner(System.in);

    public static String prompt(String message) {
        System.out.print(message);
        return scanner.nextLine().trim();
    }

    public static int promptInt(String message) {
        while (true) {
            System.out.print(message);
            String input = scanner.nextLine().trim();
            try {
                return Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println("Invalid number. Please try again.");
            }
        }
    }

    public static <T> T promptMenu(String message, List<T> options, String exitOption) {
        return promptMenu(message, IntStream.range(0, options.size()).boxed().collect(Collectors.toMap(options::get, options::get)), exitOption);
    }

    public static <T> T promptMenu(String message, List<T> options) {
        return promptMenu(message, options, null);
    }

    public static <R, T> R promptMenu(String message, Map<R, T> options, String exitOption) {
        var returnOpts = new ArrayList<>(options.keySet());
        while (true) {
            System.out.println(message);
            int n = returnOpts.size();
            for (int i = 0; i < n; i++) {
                System.out.printf("%d) %s\n", i + 1, options.get(returnOpts.get(i)));
            }
            int exitIdx = -1;
            if (exitOption != null) {
                exitIdx = n + 1;
                System.out.printf("%d) %s\n", exitIdx, exitOption);
            }
            int choice = promptInt("Select option: ");
            if (choice >= 1 && choice <= n) {
                return returnOpts.get(choice - 1);
            }
            if (exitOption != null && choice == exitIdx) {
                return null;
            }
            System.out.println("Invalid choice. Please try again.");
        }
    }

    public static <R, T> R promptMenu(String message, Map<R, T> options) {
        return promptMenu(message, options, null);
    }

    public static List<String> promptList(String message) {
        String input = prompt(message);
        if (input.isEmpty()) return List.of();
        String[] parts = input.split(",");
        List<String> result = new ArrayList<>();
        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) result.add(trimmed);
        }
        return result;
    }
} 