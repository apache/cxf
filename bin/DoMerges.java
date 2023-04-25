/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements. See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership. The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License. You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied. See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/* dkulp - Stupid little program I use to help merge changes from
   trunk to the fixes branches.   It requires the command line version of
   svn to be available on the path.   If using a git checkout, it also requires
   the command line version of git on the path.

   Basically, git does all the work, but this little wrapper
   thing will display the commit logs, prompt if you want to merge/block/ignore
   each commit, prompt for commit (so you can resolve any conflicts first),
   etc....

   Yes - doing this in python itself (or perl or even bash itself or ruby or ...)
   would probably be better.  However, I'd then need to spend time
   learning python/ruby/etc... that I just don't have time to do right now.
   What is more productive: Taking 30 minutes to bang this out in Java or
   spending a couple days learning another language that would allow me to
   bang it out in 15 minutes?

   Explanation of commands:

   [B]lock will permanently block the particular commit from being merged.
   It won't ask again on subsequent runs of DoMerge.

   [I]gnore ignores the commit for the current DoMerges run, but will ask
   again the next time you DoMerges.  If you're not certain for a particular
   commit use this option for someone else to determine on a later run.

   [R]ecord formally records that a merge occurred, but it does *not*
   actually merge the commit.  This is useful if you another tool to do
   the merging but still wish to record a merge did occur.

   [F]lush will permanently save all the [B]'s and [R]'s you've earlier made,
   useful when you need to stop DoMerges (due to a missed commit or other
   problem) before it's complete.  That way subsequent runs of DoMerges
   won't go through the blocked/recorded items again.  (Flushes occur
   automatically when DoMerges is finished running.)

   [C]hanges will display the changes in the commit to help you decide the
   appropriate action to take.

*/

public class DoMerges {
    public static final String MERGEINFOFILE = ".gitmergeinfo";

    public static boolean auto = false;
    public static Pattern jiraPattern = Pattern.compile("([A-Z]{2,10}+-\\d+)");
    public static String username;
    public static String fromBranch;

    public static Set<String> records = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
    public static Set<String> patchIds = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

    static class ToFrom {
        final String from;
        final String to;

        public ToFrom(String t, String f) {
            to = t;
            from = f;
        }
    }
    public static List<ToFrom> pathMaps = new LinkedList<>();

    static int waitFor(Process p) throws Exception  {
        return waitFor(p, true);
    }
    static int waitFor(Process p, boolean exit) throws Exception  {
        int i = p.waitFor();
        if (i != 0) {
            System.out.println("ERROR!");
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getErrorStream()));
            String line = reader.readLine();
            while (line != null) {
                System.out.println(line);
                line = reader.readLine();
            }
            if (exit) {
                System.exit(1);
            }
        }
        return i;
    }
    static int runProcess(Process p) throws Exception {
        return runProcess(p, true);
    }
    static int runProcess(Process p, boolean exit) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
        String line = reader.readLine();
        while (line != null) {
            System.out.println(line);
            line = reader.readLine();
        }
        return waitFor(p, exit);
    }


    static boolean doCommit() throws Exception {
        while (System.in.available() > 0) {
            System.in.read();
        }
        char c = auto ? 'Y' : 0;
        while (c != 'Y'
               && c != 'N') {
            System.out.print("Commit:  [Y]es, or [N]o? ");
            int i = System.in.read();
            c = Character.toUpperCase((char)i);
        }
        if (c == 'N') {
            Process p = Runtime.getRuntime().exec(new String[] {"git", "reset", "--hard"});
            runProcess(p);
            return false;
        }

        Process p = Runtime.getRuntime().exec(new String[] {"git", "commit", "--no-edit", "-a"});
        runProcess(p);
        return true;
    }

    public static void changes(String ver) throws Exception {
        Process p = Runtime.getRuntime().exec(getCommandLine(new String[] {"git", "show", ver}));
        runProcess(p);
    }

    public static void flush() throws Exception {
        BufferedWriter writer = new BufferedWriter(new FileWriter(MERGEINFOFILE));
        writer.write(fromBranch);
        writer.newLine();
        for (ToFrom ent : pathMaps) {
            writer.write("A ");
            writer.write(ent.from);
            writer.write(" ");
            writer.write(ent.to);
            writer.newLine();
        }
        for (String s : records) {
            writer.write(s);
            writer.newLine();
        }
        writer.flush();
        writer.close();

        Process p = Runtime.getRuntime().exec(getCommandLine(new String[] {"git", "commit", "-m",
                                                                           "Recording .gitmergeinfo Changes",
                                                                           MERGEINFOFILE}));
        runProcess(p);
    }
    public static void doUpdate() throws Exception {
        Process p = Runtime.getRuntime().exec(new String[] {"git", "pull", "--rebase"});
        runProcess(p);

        File file = new File(MERGEINFOFILE);
        records.clear();
        if (file.exists()) {
            BufferedReader reader = new BufferedReader(new FileReader(MERGEINFOFILE));
            String line = reader.readLine();
            fromBranch = line;
            line = reader.readLine();
            while (line != null) {
                if (line.startsWith("A ")) {
                    line = line.substring(2).trim();
                    String from = line.substring(0, line.indexOf(' '));
                    String to = line.substring(line.indexOf(' ') + 1);
                    pathMaps.add(new ToFrom(to, from));
                } else {
                    records.add(line.trim());
                }
                line = reader.readLine();
            }
            reader.close();
        }
        file = new File("patch-info");
        if (file.exists()) {
            BufferedReader reader = new BufferedReader(new FileReader("patch-info"));
            String line = reader.readLine();
            while (line != null) {
                patchIds.add(line.trim());
                line = reader.readLine();
            }
            reader.close();
        }
    }

    public static List<String> getAvailableUpdates() throws Exception {
        List<String> verList = new LinkedList<>();
        Process p;
        BufferedReader reader;
        String line;

        p = Runtime.getRuntime().exec(getCommandLine(new String[] {"git", "cherry",
                                                                   "HEAD", fromBranch}));

        reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
        line = reader.readLine();
        while (line != null) {
            if (line.charAt(0) == '+') {
                String ver = line.substring(2).trim();
                if (!records.contains("B " + ver) && !records.contains("M " + ver)) {
                    verList.add(ver);
                }
            }
            line = reader.readLine();
        }
        p.waitFor();
        return verList;
    }

    public static List<String[]> getGitLogs() throws Exception {
        BufferedReader reader;
        String line;
        Process p = Runtime.getRuntime().exec(new String[] {"git", "log", fromBranch + "..HEAD"});
        reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
        line = reader.readLine();

        List<String[]> map = new LinkedList<>();
        List<String> list = new ArrayList<>(10);
        while (line != null) {
            if (line.length() > 0 && line.startsWith("commit ")) {
                if (!list.isEmpty()) {
                    map.add(list.toArray(new String[list.size()]));
                    list.clear();
                }
                while (line != null && line.length() > 0 && line.charAt(0) != ' ') {
                    list.add(line);
                    line = reader.readLine();
                }
            }
            list.add(line);
            line = reader.readLine();
        }
        if (!list.isEmpty()) {
            map.add(list.toArray(new String[list.size()]));
            list.clear();
        }
        return map;
    }

    public static String[] getLog(String ver, Set<String> jiras) throws Exception {
        Process p;
        BufferedReader reader;
        String line;
        p = Runtime.getRuntime().exec(new String[] {"git", "log", "--pretty=medium", "-n", "1" , ver});

        reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
        line = reader.readLine();
        List<String> lines = new ArrayList<>(10);
        while (line != null) {
            if (!line.startsWith("commit ")) {
                lines.add(line);
                Matcher m = jiraPattern.matcher(line);
                while (m.find()) {
                    jiras.add(m.group());
                }
            }
            line = reader.readLine();
        }
        p.waitFor();
        return lines.toArray(new String[lines.size()]);
    }

    private static void doMerge(String ver) throws Exception {
        Process p = Runtime.getRuntime().exec(getCommandLine(new String[] {"git", "cherry-pick", "-x", ver}));
        if (runProcess(p, false) != 0) {
            p = Runtime.getRuntime().exec(getCommandLine(new String[] {"git", "status"}));
            runProcess(p);

            if (doCommit()) {
                records.add("M " + ver);
            }
        } else {
            String oldPatchId = getPatchId(ver);
            String newPatchId = getPatchId("HEAD");
            if (!oldPatchId.equals(newPatchId)) {
                records.add("M " + ver);
            }
        }
    }
    private static void doMappedMerge(String ver) throws Exception {
        Process p = Runtime.getRuntime().exec(getCommandLine(new String[] {"git", "format-patch", "--stdout", "-1", "-k", ver}));
        BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));

        File outputFile = File.createTempFile("merge", ".patch");
        outputFile.deleteOnExit();
        BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile));

        String line = reader.readLine();
        while (line != null) {
            if ((line.startsWith("--- ")
                || line.startsWith("+++ ")) && line.length() > 7) {
                String file = line.substring(6).trim();
                for (ToFrom ent : pathMaps) {
                    if (file.contains(ent.from)) {
                        String newf = file.replace(ent.from, ent.to);
                        File fo = new File(newf);
                        if (fo.exists() && fo.isFile()) {
                            line = line.substring(0, 6) + newf;
                            break;
                        }
                    }
                }
                //System.out.println("newl: " + line);
            }
            writer.append(line);
            writer.newLine();
            line = reader.readLine();
        }
        waitFor(p, false);
        writer.flush();
        writer.close();

        p = Runtime.getRuntime().exec(getCommandLine(new String[] {"git", "am", "-k", outputFile.getCanonicalPath()}));

        if (waitFor(p, false) != 0) {
            p = Runtime.getRuntime().exec(getCommandLine(new String[] {"git", "status"}));
            runProcess(p);

            if (doCommit()) {
                records.add("M " + ver);
            }
        } else {
            records.add("M " + ver);
        }
        outputFile.delete();
    }
    private static String getPatchId(String id) throws Exception {

        String[] commands = new String[] { "git", "show", id};
        Process p = Runtime.getRuntime().exec(commands);
        InputStream in = p.getInputStream();

        commands = new String[] { "git", "patch-id"};
        Process p2 = Runtime.getRuntime().exec(commands);
        OutputStream out = p2.getOutputStream();
        byte[] bytes = new byte[1024];
        int len = in.read(bytes);
        BufferedReader r2 = new BufferedReader(new InputStreamReader(p2.getInputStream()));
        while (len > 0) {
            out.write(bytes, 0, len);
            len = in.read(bytes);
        }
        p.waitFor();
        out.close();

        id = r2.readLine();
        p2.waitFor();

        id = id.substring(0, id.indexOf(' '));
        return id;
    }

    private static String getUserName() throws Exception {
        BufferedReader reader;
        String line;
        Process p = Runtime.getRuntime().exec(new String[] {"git", "config", "user.name"});

        reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
        line = reader.readLine();
        p.waitFor();
        return line;
    }

    public static void main(String[] a) throws Exception {
        File file = new File(".git-commit-message.txt");
        if (file.exists()) {
            //make sure we delete this to not cause confusion
            file.delete();
        }

        System.out.println("Updating directory");

        doUpdate();

        List<String> args = new LinkedList<>(Arrays.asList(a));
        List<String> check = new LinkedList<>();
        while (!args.isEmpty()) {
            String get = args.remove(0);

            if ("-auto".equals(get)) {
                auto = true;
            } else if ("-me".equals(get)) {
                username = getUserName();
            } else if ("-user".equals(get)) {
                username = args.get(0);
            } else {
                check.add(get);
            }
        }


        List<String> verList = getAvailableUpdates();
        if (!check.isEmpty()) {
            List<String> newList = new LinkedList<>();
            for (String s : check) {
                if (verList.contains(s)) {
                    newList.add(s);
                }
            }
            verList = newList;
        }
        if (verList.isEmpty()) {
            System.out.println("Nothing needs to be merged");
            System.exit(0);
        }

        System.out.println("Merging versions (" + verList.size() + "): " + verList);

        List<String[]> gitLogs = null;
        //with GIT, we can relatively quickly check the logs on the current branch
        //and compare with what should be merged and check if things are already merged
        gitLogs = getGitLogs();

        Set<String> jiras = new TreeSet<>();

        for (int cur = 0; cur < verList.size(); cur++) {
            jiras.clear();
            String ver = verList.get(cur);
            String[] logLines = getLog(ver, jiras);
            if (logLines.length > 1
                && username != null
                && !logLines[0].contains(username)) {
                continue;
            }
            System.out.println();
            System.out.println("Merging: " + ver + " (" + (cur + 1) + "/" + verList.size() + ")");
            //System.out.println("http://svn.apache.org/viewvc?view=revision&revision=" + ver);

            for (String s : jiras) {
                System.out.println("https://issues.apache.org/jira/browse/" + s);
            }
            StringBuilder log = new StringBuilder();
            if (isBlocked(logLines)) {
                records.add("B " + ver);
                continue;
            }
            for (String s : logLines) {
                System.out.println(s);
                log.append(s).append("\n");
            }

            char c = auto ? 'M' : 0;
            if (checkPatchId(ver)) {
                continue;
            }
            if (checkAlreadyMerged(ver, gitLogs, logLines)) {
                c = 'R';
            }

            while (System.in.available() > 0) {
                System.in.read();
            }
            while (c != 'M'
                   && c != 'A'
                   && c != 'B'
                   && c != 'I'
                   && c != 'R'
                   && c != 'F'
                   && c != 'C'
                   && c != 'P') {
                System.out.print("[M]erge, [A]dvancedMerge, [B]lock, or [I]gnore, [R]ecord only, [F]lush, [C]hanges? ");
                int i = System.in.read();
                c = Character.toUpperCase((char)i);
            }

            switch (c) {
            case 'M':
                doMerge(ver);
                break;
            case 'A':
                doMappedMerge(ver);
                break;
            case 'P':
                System.out.println("Patch Id: " + getPatchId(ver));
                cur--;
                break;
            case 'B':
                records.add("B " + ver);
                break;
            case 'R':
                records.add("M " + ver);
                break;
            case 'F':
                flush();
                cur--;
                break;
            case 'C':
                changes(ver);
                cur--;
                break;
            case 'I':
                System.out.println("Ignoring");
                break;
            }
        }
        flush();
    }
    private static boolean isBlocked(String[] logLines) {
        for (String s: logLines) {
            if (s.trim().contains("Recording .gitmergeinfo Changes")) {
                return true;
            }
            if (s.contains("[maven-release-plugin] prepare")) {
                return true;
            }
        }
        return false;
    }
    private static boolean checkPatchId(String ver) throws Exception {
        if (!patchIds.isEmpty()) {
            String pid = getPatchId(ver);
            if (patchIds.contains("B " + pid)) {
                records.add("B " + ver);
                System.out.println("Already blocked: " + ver);
                return true;
            } else if (patchIds.contains("M " + pid)) {
                records.add("M " + ver);
                System.out.println("Already merged: " + ver);
                return true;
            }
        }
        return false;
    }
    private static boolean checkAlreadyMerged(String ver, List<String[]> gitLogs, String[] logLines) throws Exception {
        if (gitLogs == null) {
            return false;
        }
        Set<List<String>> matchingLogs = new HashSet<>();
        for (String[] f : gitLogs) {
            List<String> ll = compareLogs(f, logLines);
            if (!ll.isEmpty()) {
                matchingLogs.add(ll);
            }
        }

        if (!matchingLogs.isEmpty()) {
            //everything in the source log is in a log on this branch, let's prompt to record the merge
            System.out.println("Found possible commit(s) already on branch:");
            int m = 0;
            for (List<String> f : matchingLogs) {
                for (String s : f) {
                    System.out.println("    " + s);
                }
                System.out.println("------------------------");
                if (++m == 4) {
                    break;
                }
            }

            while (System.in.available() > 0) {
                System.in.read();
            }
            char c = 0;
            while (c != 'Y'
                   && c != 'N') {
                System.out.print("Record as merged [Y/N]? ");
                int i = System.in.read();
                c = Character.toUpperCase((char)i);
            }
            if (c == 'Y') {
                return true;
            }
        }
        return false;
    }
    private static List<String> compareLogs(String[] f, String[] logLines) throws IOException {
        ArrayList<String> onBranch = new ArrayList<>(f.length);
        for (String s : f) {
            if (s.trim().startsWith("Conflicts:")) {
                break;
            }
            if (s.trim().length() > 0
                && s.charAt(0) == ' '
                && !s.contains("git-svn-id")) {
                onBranch.add(s.trim());
            }
        }

        List<String> ll = new ArrayList<>();
        for (String s : logLines) {
            if (s.trim().length() > 0
                && onBranch.remove(s.trim())
                && !s.startsWith("Author: ")
                && !s.startsWith("Date: ")
                && !s.contains("git-svn-id")) {
                ll.add(s);
            }
        }
        return ll;
    }


    private static String[] getCommandLine(String[] args) {
        List<String> argLine = new ArrayList<>();
        if (isWindows()) {
            argLine.add("cmd.exe");
            argLine.add("/c");
        }

        argLine.addAll(Arrays.asList(args));
        StringBuilder b = new StringBuilder(256);
        for (String s : argLine) {
            if (b.length() == 0) {
                b.append("Running \"");
            } else {
                b.append(' ');
            }
            b.append(s);
        }
        b.append("\"...");
        System.out.println(b.toString());
        return argLine.toArray(new String[argLine.size()]);
    }

    private static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().indexOf("windows") != -1;
    }


}


