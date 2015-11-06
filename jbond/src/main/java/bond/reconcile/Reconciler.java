package bond.reconcile;

import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.io.Files;
import difflib.Delta;
import difflib.DiffUtils;
import difflib.Patch;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static bond.reconcile.ReconcileType.CONSOLE;

public abstract class Reconciler {

  public static Reconciler getReconciler() {
    String reconcileTypeName = System.getenv("BOND_RECONCILE");
    if (reconcileTypeName == null || reconcileTypeName.equals("")) {
      return getReconciler(CONSOLE);
    }
    return getReconciler(ReconcileType.getFromName(reconcileTypeName));
  }

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

  protected abstract Optional<List<String>> reconcileDiffs(String testName, File referenceFile,
      List<String> currentLines, List<String> unifiedDiff, String noSaveMessage) throws IOException;

  protected void printDiffs(String testName, List<String> unifiedDiff) {
    if (unifiedDiff.isEmpty()) {
      printf("No differences in observations for %s\n", testName);
    } else {
      printf("There were differences in observations for %s\n", testName);
      printf(Joiner.on("\n").join(unifiedDiff) + "\n");
      // Print it again at the end; makes it easy to see in the console what test just failed
      printf("There were differences in observations for %s\n", testName);
    }
  }

  private List<String> fileToLines(File file) throws IOException {
    return Files.readLines(file, Charsets.UTF_8);
  }

  protected void printf(String formatString, Object... objs) {
    System.out.printf(formatString, objs);
  }

  // TODO this is a workaround for IDEs / Gradle redirecting stdin in such a way
  // that it's completely inaccessible during tests... Needs work, though.
  protected String getUserInput(String prompt, List<String> options) {
    JFrame frame = new JFrame("BondReconcileFrame");
    Object[] buttonText = new Object[options.size()];
    for (int i = 0; i < options.size(); i++) {
      buttonText[i] = options.get(i);
    }
    int n = JOptionPane.showOptionDialog(frame,
        prompt,
        "Bond Test Reconciliation",
        JOptionPane.YES_NO_CANCEL_OPTION,
        JOptionPane.QUESTION_MESSAGE,
        null,     //do not use a custom Icon
        buttonText,  //the titles of buttons
        buttonText[0]); //default button title
    return options.get(n);
    //String ret = System.console().readLine();
    //printf("RET: " + ret);
    //return ret;
    //Scanner in = new Scanner(System.in);
    //printf(prompt);
    //return in.nextLine();
  }

}

class KDiff3Reconciler extends Reconciler {
  protected Optional<List<String>> reconcileDiffs(String testName, File referenceFile,
                                                  List<String> currentLines, List<String> unifiedDiff, String noSaveMessage) throws IOException {

    File currentFile = new File(referenceFile.getCanonicalPath() + ".temp");
    File mergedFile = new File(referenceFile.getCanonicalPath() + ".tempmerge");
    Files.write(Joiner.on("\n").join(currentLines), currentFile, Charsets.UTF_8);
    List<String> cmd = Lists.newArrayList("kdiff3", referenceFile.getCanonicalPath(), "--L1",
        testName + "_REFERENCE", currentFile.toString(), "--L2", testName + "_CURRENT");
    if (noSaveMessage != null) {
      String response = getUserInput(String.format("\n!!! MERGING NOT ALLOWED for %s: %s. " +
                                                      "Want to start kdiff3? ([y]es | *): ",
          testName, noSaveMessage), Lists.newArrayList("yes", "no"));
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
      Thread.currentThread().interrupt(); // Set in case test runner wants to do something with it
    }
    if (noSaveMessage != null) {
      return Optional.absent();
    } else {
      if (exitCode == 0) {
        List<String> mergedLines = Files.readLines(mergedFile, Charsets.UTF_8);
        mergedFile.delete();
        return Optional.of(mergedLines);
      } else {
        if (mergedFile.exists()) {
          mergedFile.delete();
        }
        return Optional.absent();
      }
    }
  }
}

class AcceptReconciler extends Reconciler {

  protected Optional<List<String>> reconcileDiffs(String testName, File referenceFile,
      List<String> currentLines, List<String> unifiedDiff, String noSaveMessage) throws IOException {
    if (noSaveMessage == null) {
      printf("Accepting (reconcile=accept) differences for test %s", testName);
    }
    return Optional.of(currentLines);
  }
}

class AbortReconciler extends Reconciler {
  protected Optional<List<String>> reconcileDiffs(String testName, File referenceFile,
      List<String> currentLines, List<String> unifiedDiff, String noSaveMessage) throws IOException {
    if (noSaveMessage == null) {
      printDiffs(testName, unifiedDiff);
    }
    return Optional.absent();
  }

}

class ConsoleReconciler extends Reconciler {

  protected Optional<List<String>> reconcileDiffs(String testName, File referenceFile,
      List<String> currentLines, List<String> unifiedDiff, String noSaveMessage) throws IOException {
    while (true) {
      String prompt;
      String response;
      if (noSaveMessage != null) {
        prompt = String.format("Observations are shown for %s. Saving them not allowed: %s\n",
            testName, noSaveMessage)
                     + "Use the diff option to show the differences. ([k]diff3 | [d]iff | *): ";
        response = getUserInput(prompt, Lists.newArrayList("[k]diff3", "[d]iff", "no"));
      } else {
        printDiffs(testName, unifiedDiff);
        prompt = String.format("Do you want to accept the changes (%s)? ([y]es | [k]diff3 | *): ",
            testName);
        response = getUserInput(prompt, Lists.newArrayList("kdiff3", "yes", "no"));
      }
      //String response = getUserInput(prompt);
      switch (response) {
        case "k":
        case "kdiff3":
          return new KDiff3Reconciler().reconcileDiffs(testName, referenceFile, currentLines,
              unifiedDiff, noSaveMessage);
        case "y":
        case "yes":
          if (noSaveMessage == null) {
            printf("Accepting differences for test %s\n", testName);
            return Optional.of(currentLines);
          } else {
            break;
          }
        case "d":
        case "diff":
          printDiffs(testName, unifiedDiff);
          continue;
      }
      if (noSaveMessage != null) {
        printf("Rejecting differences for test %s\n", testName);
      }
      return Optional.absent();
    }
  }

}
