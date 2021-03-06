package com.guseyn.migrator.git;

import com.guseyn.broken_xml.ParsedXML;
import com.guseyn.migrator.file.FileCookBook;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class GitHub {
    public static String repoNameByRepoLink(String repoLink) {
        String[] repoLinkParts = repoLink.split("/");
        if (repoLinkParts.length < 5) {
            System.err.println("Cannot get repo name from  (" + repoLink + ") ");
            return "";
        }
        String appName = repoLinkParts[repoLinkParts.length - 1];
        if (appName.endsWith(".git")) {
            appName = appName.substring(0, appName.length() - 4);
        }
        return appName;
    }

    public static String clonedRepo(String pathToClone, String linkToClone) throws IOException, InterruptedException {
        System.out.println("==> Start cloning: " + linkToClone);
        String gitToken = new ParsedXML(FileCookBook.contentFromResources("/git-creds.xml")).document().roots().get(0).texts().get(0).value();
        String[] partsOfLinkToClone = linkToClone.split("//");
        if (partsOfLinkToClone.length < 2) {
            throw new IllegalArgumentException("git url is not correct");
        }
        String linkToCloneWithCreds = partsOfLinkToClone[0] + "//" + gitToken + "@" + partsOfLinkToClone[1];
        String cmdStr = "cd " + pathToClone + " && git clone " + linkToCloneWithCreds;
        Process p = Runtime.getRuntime().exec(new String[] { "bash", "-c", cmdStr });
        p.waitFor();
        System.out.println("<== Complete clone");
        return pathToClone;
    }

    public static String copiedRepo(String pathOfFolderWithClones, String repoName, String newCopyName) throws InterruptedException, IOException {
        System.out.println("==> Start copy: " + repoName + ", to:" + newCopyName);
        String cmdStr = "cd " + pathOfFolderWithClones + " && cp -r " + repoName + " " + newCopyName;
        Process p = Runtime.getRuntime().exec(new String[] { "bash", "-c", cmdStr });
        p.waitFor();
        System.out.println("<== copy done");
        return newCopyName;
    }

    public static void deleteFolder(String folderPath) throws InterruptedException, IOException {
        System.out.println("==> Start deleting ...");
        String cmdStr = " rm -rf " + folderPath + "";
        Process p = Runtime.getRuntime().exec(new String[] { "bash", "-c", cmdStr });
        p.waitFor();
        System.out.println("<== Complete delete");
    }

    public static String checkedoutCommit(String repoClonePath, String commitID) throws IOException, InterruptedException {
        if (commitID.contains("_")) {
            String[] commitIDSP = commitID.split("_");
            commitID = commitIDSP[1];
        }
        System.out.println("==> Start checkout: " + commitID);
        String cmdStr = "cd " + repoClonePath + " && git checkout " + commitID;
        Process p = Runtime.getRuntime().exec(new String[] { "bash", "-c", cmdStr });
        p.waitFor();
        System.out.println("<== Complete checkout");
        return commitID;
    }

    // This method find commit id from String
    public static String commitId(String commitID) {
        String[] commitParts = commitID.split("_");
        if (commitParts.length != 2) {
            return null;
        }
        return commitParts[1];
    }

    public static String generatedFileWithCommitLogs(String pathToClone, String repoName) throws IOException, InterruptedException {
        final String logFileName = "commits.txt";
        System.out.println("==> Start generate logs for: " + logFileName);
        String cmdStr = "cd " + pathToClone + "/" + repoName + " && git log --name-status  --reverse >../" + logFileName;
        Process p = Runtime.getRuntime().exec(new String[] { "bash", "-c", cmdStr });
        p.waitFor();
        System.out.println("<== Complete generate logs");
        return logFileName;
    }

    public static List<Commit> commitsFromLogFile(String pathToLogsFolder) throws IOException {
        final List<Commit> listOfCommits = new ArrayList<>();
        final ArrayList<CommitFiles> listOfCommitFiles = new ArrayList<>();
        final String logFileName = pathToLogsFolder + "/commits.txt";
        int commitNumber = 1;
        String currentLine;
        String commitID = "";
        String developerName = "";
        String commitDate = "";
        String commitText = "";
        int countOfSpaces = 0;
        try(BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(logFileName)))) {
            while ((currentLine = bufferedReader.readLine()) != null) {
                if (currentLine.startsWith("commit ")) {
                    if (!commitID.isEmpty()) {
                        listOfCommits.add(new Commit(commitID, developerName, commitDate, commitText, listOfCommitFiles));
                    }
                    commitID = currentLine.substring("commit ".length()).trim();
                    commitID = "v" + commitNumber + "_" + commitID;
                    commitNumber++;
                    countOfSpaces = 0;
                    commitText = "";
                    listOfCommitFiles.clear();
                }

                if (currentLine.startsWith("Author:")) {
                    developerName = currentLine.substring("Author:".length()).trim();
                }

                if (currentLine.startsWith("Date:")) {
                    commitDate = currentLine.substring("Date:".length()).trim();
                    commitDate = gitDateInFormat(commitDate);
                }

                if (countOfSpaces == 1) {
                    commitText = currentLine.trim();
                    countOfSpaces++;

                }

                if (currentLine.trim().length() == 0) {
                    countOfSpaces++;
                }

                if (currentLine.endsWith("pom.xml")) {
                    String[] currentLineParts = currentLine.split("\\s+");
                    String fileOperation = null;
                    String firstFile = null;
                    String secondFile = null;

                    // Not always this one is file
                    if (currentLineParts.length >= 2) {
                        fileOperation = currentLineParts[0].trim();
                        firstFile = currentLineParts[1].trim();
                    }

                    if (currentLineParts.length >= 3) {
                        secondFile = currentLineParts[2].trim();
                    }

                    if (currentLineParts.length >= 2) {
                        listOfCommitFiles.add(new CommitFiles(fileOperation, firstFile, secondFile));
                    }
                }
            }
            if (!commitID.isEmpty()) {
                // printCommitInfo( commitId, developerName, commitDate,commitText,commitFiles);
                listOfCommits.add(new Commit(commitID, developerName, commitDate, commitText, listOfCommitFiles));
            }
        }
        return listOfCommits;
    }

    private static String gitDateInFormat(String gitDate) {
        final List<String> months = new ArrayList<String>(){{
            add("January");
            add("February");
            add("March");
            add("April");
            add("May");
            add("June");
            add("July");
            add("August");
            add("September");
            add("October");
            add("November");
            add("December");
        }};
        String[] gitDateParts = gitDate.split(" ");
        for (int i = 0; i < 12; i++) {
            if (months.get(i).contains(gitDateParts[1])) {
                return gitDateParts[4] + "-" + ((i + 1) < 10 ? "0" : "") + (i + 1) + "-"
                    + (Integer.parseInt(gitDateParts[2]) < 10 ? "0" : "") + gitDateParts[2] + " " + gitDateParts[3];
            }
        }
        return "";
    }

    public static boolean areTwoCommitsSequential(String oldCommitId, String newCommitId) {
        String[] newCommitIdParts = newCommitId.split("_");
        String[] oldCommitIdParts = oldCommitId.split("_");
        if (newCommitIdParts.length < 1 || oldCommitIdParts.length < 1 || newCommitIdParts[0].isEmpty() || oldCommitIdParts[0].isEmpty()) {
            return true;
        }
        int newCommitNumber = Integer.parseInt(newCommitIdParts[0].substring(1));
        int oldCommitNumber = Integer.parseInt(oldCommitIdParts[0].substring(1));
        if ((newCommitNumber - oldCommitNumber) >= 0) {
            return true;
        } else {
            System.err.println("Missing version=" + (newCommitNumber - oldCommitNumber));
            return false;
        }
    }

    public static boolean doesRepoExist(String repoName) {
        String pathOfFolderWithClones = Paths.get(".").toAbsolutePath().normalize().toString() + "/Clone/";
        String repoPath = pathOfFolderWithClones + repoName;
        return Files.isDirectory(Paths.get(repoPath));
    }

    // get list of changed files at specific commit
    public static ArrayList<String> listOfChangedFiles(String pathOfFolderWithClones, String commitId) throws IOException {
        commitId = commitId(commitId);
        ArrayList<String> listOfChangedFiles = new ArrayList<String>();
        String fileWithCommitsPath = pathOfFolderWithClones + "app_commits.txt";
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(fileWithCommitsPath)));
        String line;
        boolean commitInfo = false;
        while ((line = br.readLine()) != null) {
            if (commitInfo) {
                if (line.startsWith("M") && line.endsWith(".java")) {
                    listOfChangedFiles.add(line.substring(2).trim());
                }
            }
            if (commitId != null && line.contains(commitId)) {
                commitInfo = true;
            } else if (line.startsWith("commit")) {
                commitInfo = false;
            }
        }
        return listOfChangedFiles;
    }
}
