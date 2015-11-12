package bond.reconcile;

import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.io.Files;
import difflib.Delta;
import difflib.DiffUtils;
import difflib.Patch;

import java.io.Console;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>Class to reconcile differences between the reference set of observations and
 * the current set of observations.</p>
 *
 * <p>Should be subclassed to implement different types of reconciliation logic.</p>
 */
public abstract class Reconciler {

  /**
   * Retrieve the correct {@code Reconciler} for {@code type}.
   *
   * @param type The type to fetch a {@code Reconciler} for
   * @return The proper {@code Reconciler}
   */
  public static Reconciler getReconciler(ReconcileType type) {
    switch (type) {
      case ABORT:
        return new AbortReconciler();
      case ACCEPT:
        return new AcceptReconciler();
      case CONSOLE:
        return new ConsoleReconciler();
      case KDIFF3:
        return new KDiff3Reconciler();
      default:
        throw new IllegalArgumentException(type + " is not a valid reconciliation tool!");
    }
  }

  /**
   * <p>The main entrypoint to the class; performs reconciliation.</p>
   *
   * <p>If reconciliation was successful and {@code noSaveMessage} is null, the reconciled
   * set of observations are saved to {@code referenceFile}, overriding the previous
   * set of reference observations.</p>
   *
   * @param testName The name of the current test
   * @param referenceFile File containing the set of reference observations; if this file
   *                      does not exist, it will be treated as if it was empty.
   * @param currentLines A list of all of the current observations, broken into single-line
   *                     strings.
   * @param noSaveMessage If the test failed, a String explaining the failure. Else, null.
   * @return True iff the reconciliation was successful
   * @throws IOException If errors are encountered while writing out reconciled observations
   */
  public boolean reconcile(String testName, File referenceFile, List<String> currentLines,
                           String noSaveMessage) throws IOException {

    List<String> referenceLines;
    if (!referenceFile.exists()) {
      referenceLines = new ArrayList<>();
      printf("WARNING: No reference observation file found for %s: %s\n", testName, referenceFile);
    } else {
      referenceLines = fileToLines(referenceFile);
    }
    Patch patch = DiffUtils.diff(referenceLines, currentLines);
    List<String> unifiedDiffs = DiffUtils.generateUnifiedDiff("reference", "current", referenceLines, patch, 4);
    List<Delta> deltas = patch.getDeltas();

    // No differences, nothing to be done
    if (deltas.isEmpty()) {
      return true;
    }

    Optional<List<String>> mergedStrings = reconcileDiffs(testName, referenceFile,
        currentLines, unifiedDiffs, noSaveMessage);

    if (mergedStrings.isPresent()) {
      if (noSaveMessage != null) {
        printf("Not saving reference observation file for %s: %s\n", testName, noSaveMessage);
      } else {
        printf("Saving updated reference observation file for %s\n", testName);
        if (referenceFile.exists()) {
          referenceFile.delete();
        }
        Files.write(Joiner.on("\n").join(mergedStrings.get()), referenceFile, Charsets.UTF_8);
      }
      return true;
    } else {
      return false;
    }
  }

  /**
   * The primary reconciliation logic.
   *
   * @param testName The name of the current test.
   * @param referenceFile The file containing the current set of reference observations
   * @param currentLines A list of all of the current observations, broken into single-line
   *                     strings.
   * @param unifiedDiff A list of all lines of the unified diff between {@code referenceFile}
   *                    and {@code currentLines}
   * @param noSaveMessage If the test failed, a String explaining the failure. Else, null.
   * @return {@link Optional#absent()} if the reconciliation failed; in this case no new
   *         observations will be saved. Else, return a list of all of the lines that
   *         should be saved as the new reference observations.
   * @throws IOException If errors are encountered while writing out reconciled observations
   */
  protected abstract Optional<List<String>> reconcileDiffs(String testName, File referenceFile,
      List<String> currentLines, List<String> unifiedDiff, String noSaveMessage) throws IOException;

  /**
   * Prepare a full diff string to be printed based off of the test name and
   * list of unified diff lines.
   *
   * @param testName Test name
   * @param unifiedDiff List of unified diff lines
   * @return String to be printed to show the current diffs
   */
  protected String getDiffString(String testName, List<String> unifiedDiff) {
    if (unifiedDiff.isEmpty()) {
      return String.format("No differences in observations for %s\n", testName);
    } else {
      return String.format("There were differences in observations for %s\n", testName) +
                 Joiner.on("\n").join(unifiedDiff) + "\n" +
                 // Print it again at the end; makes it easy to see in the console what test just failed
                 String.format("There were differences in observations for %s\n", testName);
    }
  }

  private List<String> fileToLines(File file) throws IOException {
    return Files.readLines(file, Charsets.UTF_8);
  }

  /**
   * Used internally to print to the console; this should be used for any printing
   * performed within a {@code Reconciler}
   *
   * @param formatString Formatting string, a la {@code PrintStream}'s {@code printf}
   * @param objs Objects to be formatted
   */
  protected void printf(String formatString, Object... objs) {
    System.out.printf(formatString, objs);
  }

  /**
   * Retrieve user input. Attempts to use the console; if one cannot be found,
   * instead creates a dialog box to request input.
   *
   * @param prompt The prompt to be displayed to the user.
   * @param options List of options to be presented to the user. Must be at least one.
   *                The last option in the list will be the default.
   * @param singleCharOptions The single-character shortcut to each one of the
   *                          options, which <b>must</b> appear somewhere in the option
   *                          itself.
   * @return The option which was selected by the user.
   */
  protected String getUserInput(String prompt, List<String> options, List<String> singleCharOptions) {
    return getUserInput(prompt, options, singleCharOptions, "");
  }

  /**
   * Retrieve user input. Attempts to use the console; if one cannot be found,
   * instead creates a dialog box to request input.
   *
   * @param prompt The prompt to be displayed to the user.
   * @param options List of options to be presented to the user. Must be at least one.
   *                The last option in the list will be the default.
   * @param singleCharOptions The single-character shortcut to each one of the
   *                          options, which <b>must</b> appear somewhere in the option
   *                          itself.
   * @param extraPromptForDialog If a dialog box is used instead of a console to request
   *                             user input, display this string before {@code prompt}
   * @return The option which was selected by the user.
   */
  protected String getUserInput(String prompt, List<String> options,
      List<String> singleCharOptions, String extraPromptForDialog) {
    if (options.size() != singleCharOptions.size()) {
      throw new IllegalArgumentException("options and singleCharOptions must have same size!");
    }
    Console sysConsole = System.console();
    if (sysConsole == null) {
      return getUserInputFromDialog(extraPromptForDialog + "\n" + prompt, options);
    } else {
      return getUserInputFromConsole(prompt, options, singleCharOptions);
    }
  }

  /**
   * Create a dialog to retrieve user input.
   *
   * @param prompt Text to display
   * @param options Options to display as buttons, with the last option being the
   *                default.
   * @return The selected option
   */
  private String getUserInputFromDialog(String prompt, List<String> options) {
    return ReconcileDialog.showDialogGetValue(prompt, options);
  }

  /**
   * Retrieve user input from the console.
   *
   * @param prompt Text to display to the user
   * @param options Options to display to the user, with the last option
   *                being the default.
   * @param singleCharOptions The single-character shortcut to each one of the
   *                          options, which <b>must</b> appear somewhere in the option
   *                          itself.
   * @return The selected option
   */
  private String getUserInputFromConsole(String prompt, List<String> options,
      List<String> singleCharOptions) {
    List<String> optionsWithSingleChar = new ArrayList<>();
    for (int i = 0; i < options.size(); i++) {
      String output = createOptionWithSingleChar(options.get(i), singleCharOptions.get(i));
      if (i == options.size() - 1) { // default option; highlight in bold
        output = "\\e[1m" + output + "\\e[0m";
      }
      optionsWithSingleChar.add(output);
    }
    printf(prompt + "(" + Joiner.on(" | ").join(optionsWithSingleChar) + " ): ");
    String input = System.console().readLine();
    if (input.length() == 1 && singleCharOptions.contains(input)) {
      return options.get(singleCharOptions.indexOf(input));
    } else if (input.equals("")) {
      return options.get(options.size() - 1);
    }
    return input;
  }

  /**
   * Combine an option with its single character form. For example, if
   * the option was `diff`, and the single character form was `d`, return `[d]iff`.
   *
   * @param option Full name of option
   * @param singleChar Single-character form of option
   * @return The unified option
   */
  private String createOptionWithSingleChar(String option, String singleChar) {
    return option.replaceFirst(singleChar, "[" + singleChar + "]");
  }

}

/**
 * A {@code Reconciler} which uses the kdiff3 graphical utility to merge differences.
 */
class KDiff3Reconciler extends Reconciler {

  protected Optional<List<String>> reconcileDiffs(String testName, File referenceFile,
      List<String> currentLines, List<String> unifiedDiff, String noSaveMessage)
      throws IOException {

    File currentFile = new File(referenceFile.getCanonicalPath() + ".temp");
    File mergedFile = new File(referenceFile.getCanonicalPath() + ".tempmerge");
    try {
      Files.write(Joiner.on("\n").join(currentLines), currentFile, Charsets.UTF_8);
      if (!referenceFile.exists()) {
        // In case there is no existing reference file (kdiff3 will complain otherwise)
        Files.write("\n", referenceFile, Charsets.UTF_8);
      }
      List<String> cmd = Lists.newArrayList("kdiff3", referenceFile.toString(), "--L1",
          testName + "_REFERENCE", currentFile.toString(), "--L2", testName + "_CURRENT");
      if (noSaveMessage != null) {
        String response = getUserInput(String.format("\n!!! MERGING NOT ALLOWED for %s: %s. Want to start kdiff3?",
            testName, noSaveMessage), Lists.newArrayList("yes", "no"), Lists.newArrayList("y", "n"));
        if (!(response.equals("y") || response.equals("yes"))) {
          return Optional.absent();
        }
      } else {
        cmd.add(1, "-m");
        cmd.add("-o");
        cmd.add(mergedFile.toString());
      }
      Process kdiff3Process;
      try {
        kdiff3Process = new ProcessBuilder().command(cmd).start();
      } catch (IOException e) {
        throw new RuntimeException("Cannot find kdiff3 installed on your system!");
      }
      int exitCode = 1;
      try {
        exitCode = kdiff3Process.waitFor();
      } catch (InterruptedException e) {
        // Set interrupt status in  case test runner wants to do something with it
        Thread.currentThread().interrupt();
      }
      if (noSaveMessage != null) {
        return Optional.absent();
      } else {
        if (exitCode == 0) {
          List<String> mergedLines = Files.readLines(mergedFile, Charsets.UTF_8);
          return Optional.of(mergedLines);
        } else {
          return Optional.absent();
        }
      }
    } finally {
      if (currentFile.exists()) {
        currentFile.delete();
      }
      if (mergedFile.exists()) {
        mergedFile.delete();
      }
      File kdiff3TempFile = new File(mergedFile + ".orig");
      if (kdiff3TempFile.exists()) {
        kdiff3TempFile.delete();
      }
    }
  }
}

/**
 * A {@code Reconciler} which accepts all new observations.
 */
class AcceptReconciler extends Reconciler {

  protected Optional<List<String>> reconcileDiffs(String testName, File referenceFile,
      List<String> currentLines, List<String> unifiedDiff, String noSaveMessage) throws IOException {
    if (noSaveMessage == null) {
      printf("Accepting (reconcile=accept) differences for test %s", testName);
    }
    return Optional.of(currentLines);
  }
}

/**
 * A {@code Reconciler} which does not accept any new observations.
 */
class AbortReconciler extends Reconciler {
  protected Optional<List<String>> reconcileDiffs(String testName, File referenceFile,
      List<String> currentLines, List<String> unifiedDiff, String noSaveMessage) throws IOException {
    if (noSaveMessage == null) {
      printf(getDiffString(testName, unifiedDiff));
    }
    return Optional.absent();
  }

}

/**
 * A {@code Reconciler} which requests input from the user via the console, or, if
 * none is present, through a modal dialog box.
 */
class ConsoleReconciler extends Reconciler {

  protected Optional<List<String>> reconcileDiffs(String testName, File referenceFile,
      List<String> currentLines, List<String> unifiedDiff, String noSaveMessage) throws IOException {
    String diffMessage = null;
    while (true) {
      String prompt;
      String response;
      if (noSaveMessage != null) {
        prompt = String.format("Observations are shown for %s. Saving them not allowed: %s\n",
            testName, noSaveMessage) + "Use the diff option to show the differences.";
        response = getUserInput(prompt, Lists.newArrayList("kdiff3", "diff", "okay"),
            Lists.newArrayList("k", "d", "o"),
            diffMessage == null ? Joiner.on("\n").join(currentLines) : diffMessage);
      } else {
        printf(getDiffString(testName, unifiedDiff));
        prompt = String.format("Do you want to accept the changes (%s)?", testName);
        response = getUserInput(prompt, Lists.newArrayList("kdiff3", "yes", "no"),
            Lists.newArrayList("k", "y", "n"), getDiffString(testName, unifiedDiff));
      }
      switch (response) {
        case "kdiff3":
          return new KDiff3Reconciler().reconcileDiffs(testName, referenceFile, currentLines,
              unifiedDiff, noSaveMessage);
        case "yes":
          if (noSaveMessage == null) {
            printf("Accepting differences for test %s\n", testName);
            return Optional.of(currentLines);
          } else {
            break;
          }
        case "diff":
          diffMessage = getDiffString(testName, unifiedDiff);
          printf(diffMessage);
          continue;
      }
      if (noSaveMessage != null) {
        printf("Rejecting differences for test %s\n", testName);
      }
      return Optional.absent();
    }
  }

}
