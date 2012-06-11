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

   Basically, svn does all the work, but this little wrapper 
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
   the merging (such as Git) but still wish to record a merge did occur.

   [F]lush will permanently save all the [B]'s and [R]'s you've earlier made, 
   useful when you need to stop DoMerges (due to a missed commit or other 
   problem) before it's complete.  That way subsequent runs of DoMerges 
   won't go through the blocked/recorded items again.  (Flushes occur
   automatically when DoMerges is finished running.)

   [C]hanges will display the changes in the commit to help you decide the 
   appropriate action to take.

*/

public class DoMerges {
    public static boolean auto = false;
    public static boolean isGit = false;
    public static Pattern jiraPattern = Pattern.compile("([A-Z]{2,10}+-\\d+)");

    public static String propSource;
    public static String svnSource;
    public static String svnDest;
    public static String svnRoot;
    public static String gitSource;
    
    public static Ranges merged = new Ranges();
    public static Ranges blocked = new Ranges();
    
    public static String maxRev;
    
    static class Ranges extends TreeSet<Range> {
        private static final long serialVersionUID = 1L;
        
        public void addRange(Range r) {
            add(r);
        }
        public boolean isInRange(int i) {
            for (Range r2 : this) {
                if (r2.contains(i)) {
                    return true;
                }
            }
            return false;
        }
        public String toProperty() {
            StringBuilder b = new StringBuilder(propSource);
            b.append(":");
            boolean first = true;
            for (Range r : this) {
                if (!first) {
                    b.append(',');
                } else {
                    first = false;
                }
                b.append(r.toString());
            }
            return b.toString();
        }
        public void optimize(Ranges blocked, Set<Integer> ignores) {
            Iterator<Range> it = this.iterator();
            if (!it.hasNext()) {
                return;
            }
            Range last = it.next();
            while (it.hasNext()) {
                Range r = it.next();
                if ((last.max + 1) == r.min) {
                    last.max = r.max;
                    it.remove();
                } else {
                    last = r;
                }
                /*
                while (last.max < r.min) {
                    if (!blocked.isInRange(last.max + 1)
                        && !ignores.contains(last.max + 1)) {
                        last.max++;
                    } else {
                        break;
                    }
                }
                if (last.max == r.min) {
                    last.max = r.max;
                    it.remove();
                } else {
                    last = r;
                }
                */
            }
        }
    }
    
    static class Range implements Comparable<Range> {
        int min, max;
        
        public Range(int s) {
            min = s;
            max = s;
        }
        public Range(String s) {
            int idx = s.indexOf('-');
            if (idx == -1) {
                min = Integer.parseInt(s);
                max = min;
            } else {
                min = Integer.parseInt(s.substring(0, idx));
                max = Integer.parseInt(s.substring(idx + 1));
            }
        }

        public boolean contains(int i) {
            return i >= min && i <= max;
        }
        
        public String toString() {
            if (min == max) {
                return Integer.toString(min);
            } 
            return Integer.toString(min) + "-" + Integer.toString(max);
        }

        public int compareTo(Range o) {
            return Integer.valueOf(min).compareTo(Integer.valueOf(o.min));
        }
    }


    static void waitFor(Process p) throws Exception  {
        waitFor(p, true);
    }
    static void waitFor(Process p, boolean exit) throws Exception  {
        if (p.waitFor() != 0) {
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
    }
    static void runProcess(Process p) throws Exception {
        runProcess(p, true);
    }
    static void runProcess(Process p, boolean exit) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
        String line = reader.readLine();
        while (line != null) {
            System.out.println(line);
            line = reader.readLine();
        }
        waitFor(p, exit);
    }
    
    static void initSvnInfo() throws Exception {
        Process p;
        if (isGit) { 
            p = Runtime.getRuntime().exec(new String[] {"git", "svn", "info", "."});
        } else {
            p = Runtime.getRuntime().exec(new String[] {"svn", "info", "."});
        }
        BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
        String line = reader.readLine();
        while (line != null) {
            if (line.startsWith("Repository Root: ")) {
                svnRoot = line.substring("Repository Root: ".length()).trim();
            } else if (line.startsWith("URL: ")) {
                svnDest = line.substring(5).trim();
            }
            line = reader.readLine();
        } 
        p.waitFor();
        
        
        if (isGit) { 
            p = Runtime.getRuntime().exec(new String[] {"git", "svn", "propget", "svnmerge-integrated", "."});
        } else {
            p = Runtime.getRuntime().exec(new String[] {"svn", "propget", "svnmerge-integrated", "."});
        }
        reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
        line = reader.readLine();
        while (line != null) {
            int idx = line.indexOf(':');
            if (idx != -1) {
                propSource = line.substring(0, idx);
                svnSource = svnRoot + propSource;
                if (isGit) {
                    gitSource = line.substring(0, idx);
                    if (gitSource.contains("/")) {
                        gitSource = gitSource.substring(gitSource.lastIndexOf('/') + 1);
                    }
                    gitSource = "origin/" + gitSource;
                }
                parseRevs(line.substring(idx + 1), merged);
            }
            line = reader.readLine();
        } 
        p.waitFor();
        
        if (isGit) { 
            p = Runtime.getRuntime().exec(new String[] {"git", "svn", "propget", "svnmerge-blocked", "."});
        } else {
            p = Runtime.getRuntime().exec(new String[] {"svn", "propget", "svnmerge-blocked", "."});
        }
        reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
        line = reader.readLine();
        while (line != null) {
            int idx = line.indexOf(':');
            parseRevs(line.substring(idx + 1), blocked);
            line = reader.readLine();
        } 
        p.waitFor();
        
        
        p = Runtime.getRuntime().exec(new String[] {"svn", "info", svnSource});
        reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
        line = reader.readLine();
        while (line != null) {
            if (line.startsWith("Revision: ")) {
                maxRev = line.substring("Revision: ".length()).trim();
            }
            line = reader.readLine();
        } 
        p.waitFor();
    }
    
    private static void parseRevs(String revs, Ranges ranges) {
        String sp[] = revs.split(",");
        for (String s : sp) {
            ranges.addRange(new Range(s));
        }
    }



    static void removeSvnMergeInfo() throws Exception {
        if (isGit) {
            return;
        }
        Process p = Runtime.getRuntime().exec(new String[] {"svn", "st", "."});
        BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
        List<String> list = new ArrayList<String>();
        String line = reader.readLine();
        while (line != null) {
            if (line.charAt(1) == 'M') {
                list.add(line.substring(5).trim());
            } else if (line.charAt(1) == 'C' && line.charAt(0) != 'C') {
                Process p2 = Runtime.getRuntime().exec(new String[] {"svn", "resolved", line.substring(5).trim()});
                if (p2.waitFor() != 0) {
                    Thread.sleep(10);
                }

                list.add(line.substring(5).trim());
            }
            line = reader.readLine();
        }
        p.waitFor();

        for (String s : list) { 
            p = Runtime.getRuntime().exec(new String[] {"svn", "propdel", "svn:mergeinfo", s});
            reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            line = reader.readLine();
            while (line != null) {
                line = reader.readLine();
            }
            p.waitFor();
        }
    }

    static boolean doCommit(int ver, String log) throws Exception {
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
            return false;
        }
        if (!isGit) {
            Process p = Runtime.getRuntime().exec(new String[] {"svn", "resolved", "."});
            if (p.waitFor() != 0) {
                Thread.sleep(10);
            }
        }
        
        File file = createLog(ver, log);
        Process p;
        if (isGit) {
            p = Runtime.getRuntime().exec(new String[] {"git", "commit", "-a", "-F", file.toString()});
        } else {
            p = Runtime.getRuntime().exec(new String[] {"svn", "commit", "-F", file.toString()});
            runProcess(p);
            p = Runtime.getRuntime().exec(new String[] {"svn", "up"});
        }
        runProcess(p);
        return true;
    }   
    
    
    private static File getLogFile(String action, String vers, List<VerLog> records) throws Exception {
        File file = File.createTempFile("domerge", ".log");
        file.deleteOnExit();
        BufferedWriter writer = new BufferedWriter(new FileWriter(file));
        writer.write(action);
        writer.write(" revisions ");
        writer.write(vers);
        writer.write(" via ");
        if (isGit) {
            writer.write(" git from\n");
        } else {
            writer.write(" svn from\n");            
        }
        writer.write(svnSource);
        writer.write("\n\n");
        for (VerLog l : records) {
            writer.write("........\n");
            BufferedReader reader = new BufferedReader(new StringReader(l.log));
            String line = reader.readLine();
            while (line != null) {
                if (!line.startsWith("--------")) {
                    writer.write("  ");
                    writer.write(line);
                    writer.write("\n");
                }
                line = reader.readLine();
            }
            writer.write("........\n");
        }
        writer.close();
        return file;
    }
    
    private static File createLog(int ver, String log) throws Exception {
        File file = File.createTempFile("domerge", ".log");
        file.deleteOnExit();
        BufferedWriter writer = new BufferedWriter(new FileWriter(file));
        BufferedReader reader = new BufferedReader(new StringReader(log));
        writer.write("Merged revisions ");
        writer.write(Integer.toString(ver));
        writer.write(" via ");
        if (isGit) {
            writer.write(" git cherry-pick from\n");
        } else {
            writer.write(" svn merge from\n");            
        }
        writer.write(svnSource);
        writer.write("\n\n");
        writer.write("........\n");
        String line = reader.readLine();
        while (line != null) {
            if (!line.startsWith("--------")) {
                writer.write("  ");
                writer.write(line);
                writer.write("\n");
            }
            line = reader.readLine();
        }
        writer.write("........\n");
        writer.close();
        return file;
    }

    public static void changes(int ver) throws Exception {
        Process p;
        if (isGit) {
            String id = getGitVersion(ver);
            p = Runtime.getRuntime().exec(getCommandLine(new String[] {"git", "diff", id + "^", id, gitSource}));
        } else {
            p = Runtime.getRuntime().exec(getCommandLine(new String[] {"svn", "diff", "-c", Integer.toString(ver), svnRoot}));
        }
        runProcess(p);
    }

    private static String getGitVersion(int ver) throws Exception {
        Process p;
        BufferedReader reader;
        String line;

        p = Runtime.getRuntime().exec(getCommandLine(new String[] {"git", "svn", "find-rev", "r" + ver, gitSource}));
        reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
        line = reader.readLine();
        String version = null;
        while (line != null) {
            line = line.trim();
            if (version == null && line.length() > 0) {
                version = line;
            }
            line = reader.readLine();
        }
        waitFor(p);
        return version;
    }

    public static void flush(List<VerLog> blocks, List<VerLog> records) throws Exception {
        Process p;
        BufferedReader reader;
        String line;
        File checkout = new File(".");
        if (isGit) {
            p = Runtime.getRuntime().exec(getCommandLine(new String[] {"git", "svn", "dcommit"}));
            reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            line = reader.readLine();
            String version = null;
            while (line != null) {
                line = line.trim();
                if (version == null && line.length() > 0) {
                    version = line;
                }
                line = reader.readLine();
            }
            waitFor(p);
            
            if (!records.isEmpty() || !blocks.isEmpty()) {
                checkout = File.createTempFile("gitsvn", ".co");
                checkout.delete();
                final File deleteDir = checkout;
                Runtime.getRuntime().addShutdownHook(new Thread() {
                    public void run() {
                        deleteDirectory(deleteDir);
                    }
                    
                });
                
                p = Runtime.getRuntime().exec(getCommandLine(new String[] {"svn", "co", "--depth", "empty", 
                                                                           svnDest, checkout.toString()}));
                runProcess(p);
            } 
        }
        
        if (!records.isEmpty()) {
            StringBuilder ver = new StringBuilder();
            for (VerLog s : records) {
                if (ver.length() > 0) {
                    ver.append(',');
                }
                ver.append(Integer.toString(s.ver));
                merged.addRange(new Range(s.ver));
            }
            System.out.println("Recording " + ver);
            File logF = getLogFile("Recording", ver.toString(), new ArrayList<VerLog>());
            p = Runtime.getRuntime().exec(getCommandLine(new String[] {"svn", "propset",
                                                                       "svnmerge-integrated",
                                                                       merged.toProperty(),
                                                                       checkout.toString()}));
            runProcess(p);
            
            p = Runtime.getRuntime().exec(getCommandLine(new String[] {"svn", "commit",
                                                                       "-F",
                                                                       logF.toString(),
                                                                       checkout.toString()}));
            runProcess(p);
        }

        if (!blocks.isEmpty()) {
            StringBuilder ver = new StringBuilder();
            for (VerLog s : blocks) {
                if (ver.length() > 0) {
                    ver.append(',');
                }
                ver.append(Integer.toString(s.ver));
                blocked.addRange(new Range(s.ver));
            }
            System.out.println("Blocking " + ver);
            File logF = getLogFile("Blocking", ver.toString(), blocks);
            p = Runtime.getRuntime().exec(getCommandLine(new String[] {"svn", "propset",
                                                                       "svnmerge-blocked",
                                                                       blocked.toProperty(),
                                                                       checkout.toString()}));
            runProcess(p);
            p = Runtime.getRuntime().exec(getCommandLine(new String[] {"svn", "commit",
                                                                       "-F",
                                                                       logF.toString(),
                                                                       checkout.toString()}));
            runProcess(p);
            p = Runtime.getRuntime().exec(new String[] {"svn", "up"});
            runProcess(p);
        }
        blocks.clear();
        records.clear();
    }

    public static void doUpdate() throws Exception {
        if (isGit) {
            Process p = Runtime.getRuntime().exec(new String[] {"git", "pull"});
            runProcess(p);
            p = Runtime.getRuntime().exec(new String[] {"git", "svn", "rebase"});
            runProcess(p);
        } else {
            Process p = Runtime.getRuntime().exec(new String[] {"svn", "up", "-r", "head"});
            runProcess(p);
        }
    }

    public static Set<Integer> getAvailableUpdates() throws Exception {
        Set<Integer> verList = new TreeSet<Integer>();
        Process p;
        BufferedReader reader;
        String line;
        
        String min = Integer.toString(merged.first().max);
        if (isGit) {
            p = Runtime.getRuntime().exec(getCommandLine(new String[] {"git", "svn", "log", 
                                                                       "--oneline", "-r",
                                                                       min + ":" + maxRev,
                                                                       gitSource})); 
        } else {
            p = Runtime.getRuntime().exec(getCommandLine(new String[] {"svn", "log", 
                                                                       "--quiet", "-r",
                                                                       min + ":" + maxRev,
                                                                       svnSource})); 
        }
        
        reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
        line = reader.readLine();
        while (line != null) {
            if (line.charAt(0) == 'r') {
                line = line.substring(0, line.indexOf(' ')).substring(1).trim();
                int ver = Integer.parseInt(line);
                if (!merged.isInRange(ver) && !blocked.isInRange(ver)) {
                    verList.add(ver);
                }
            }
            line = reader.readLine();
        }
        p.waitFor();
        return verList;
    }



    public static String getLog(Integer ver, Set<String> jiras) throws Exception {
        Process p;
        BufferedReader reader;
        String line;
        if (isGit) { 
            p = Runtime.getRuntime().exec(new String[] {"git", "svn", "log", "-r" , ver.toString(), gitSource});
        } else {
            p = Runtime.getRuntime().exec(new String[] {"svn", "log", "-r" , ver.toString(), svnRoot});
        }
        reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
        line = reader.readLine();
        StringWriter swriter = new StringWriter();
        BufferedWriter writer = new BufferedWriter(swriter);
        while (line != null) {
            writer.write(line);
            writer.newLine();
            Matcher m = jiraPattern.matcher(line);
            while (m.find()) {
                jiras.add(m.group());
            }
            line = reader.readLine();
        }
        p.waitFor();
        writer.flush();
        return swriter.toString();
    }
    
    private static void doMerge(int ver, String log, List<VerLog> records) throws Exception {
        Process p;
        
        if (isGit) {
            String id = getGitVersion(ver);
            p = Runtime.getRuntime().exec(getCommandLine(new String[] {"git", "cherry-pick",
                                                                       "--no-commit", 
                                                                       id}));
        } else {
            p = Runtime.getRuntime().exec(getCommandLine(new String[] {"svn", "merge", "--non-interactive",
                                                                       "-c", Integer.toString(ver), svnSource}));
        }
        runProcess(p, false);
        
        if (!isGit) {
            removeSvnMergeInfo();
            
            Range r = new Range(ver);
            merged.add(r);
            p = Runtime.getRuntime().exec(new String[] {"svn", "propset", "svnmerge-integrated", 
                                                        merged.toProperty(), "."});
            removeSvnMergeInfo();
            runProcess(p);

            if (!doCommit(ver, log)) {
                merged.remove(r);                
            }
        } else {
            p = Runtime.getRuntime().exec(getCommandLine(new String[] {"git", "status"}));
            runProcess(p);
            
            if (doCommit(ver, log)) {
                records.add(new VerLog(ver, log));
            }
        }
    }

    static class VerLog {
        int ver;
        String log;
        
        public VerLog(int v, String l) {
            ver = v;
            log = l;
        }
    }
    
    public static void main (String args[]) throws Exception {
        File file = new File("svnmerge-commit-message.txt");
        if (file.exists()) {
            //make sure we delete this to not cause confusion
            file.delete();
        }
        
        int onlyVersion = -1;
        if (args.length > 0) {
            if ("-auto".equals(args[0])) { 
                auto = true;
            } else {
                onlyVersion = Integer.valueOf(args[0]);
            }
        }
        file = new File(".git");
        if (file.exists() && file.isDirectory()) {
            isGit = true;
        }

        System.out.println("Updating directory");

        doUpdate();
        initSvnInfo();
        
        Set<Integer> verList = getAvailableUpdates();
        if (onlyVersion != -1) {
            if (!verList.contains(onlyVersion)) {
                System.out.println("Version: " + onlyVersion + " does not need merging");
                System.exit(0);
            }
            verList.clear();
            verList.add(onlyVersion);
        }

        System.out.println("Merging versions (" + verList.size() + "): " + verList);

        List<VerLog> blocks = new ArrayList<VerLog>();
        List<VerLog> records = new ArrayList<VerLog>();
        Set<Integer> ignores = new TreeSet<Integer>();
        Set<String> jiras = new TreeSet<String>();

        Integer verArray[] = verList.toArray(new Integer[verList.size()]);
        for (int cur = 0; cur < verArray.length; cur++) {
            jiras.clear();
            int ver = verArray[cur];
            System.out.println("Merging: " + ver + " (" + (cur + 1) + "/" + verList.size() + ")");
            System.out.println("http://svn.apache.org/viewvc?view=revision&revision=" + ver);
            
            String log = getLog(ver, jiras);
            
            for (String s : jiras) {
                System.out.println("https://issues.apache.org/jira/browse/" + s);
            }
            System.out.println(log);

            while (System.in.available() > 0) {
                System.in.read();
            }
            char c = auto ? 'M' : 0;
            while (c != 'M'
                   && c != 'B'
                   && c != 'I'
                   && c != 'R'
                   && c != 'F'
                   && c != 'C') {
                System.out.print("[M]erge, [B]lock, or [I]gnore, [R]ecord only, [F]lush, [C]hanges? ");
                int i = System.in.read();
                c = Character.toUpperCase((char)i);
            }

            switch (c) {
            case 'M':
                doMerge(ver, log, records);
                break;
            case 'B':
                blocks.add(new VerLog(ver, log));
                break;
            case 'R':
                records.add(new VerLog(ver, log));
                break;
            case 'F':
                flush(blocks, records);
                cur--;
                break;
            case 'C':
                changes(ver);
                cur--;
                break;
            case 'I':
                System.out.println("Ignoring");
                ignores.add(Integer.valueOf(ver));
                break;
            }
        }
        optimizeRanges(ignores);
        flush(blocks, records);
    }

    private static void optimizeRanges(Set<Integer> ignores) {
        merged.optimize(blocked, ignores);
        blocked.optimize(merged, ignores);
    }
    private static String[] getCommandLine(String[] args) {
        List<String> argLine = new ArrayList<String>();
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
                b.append(" ");
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
    
    
    private static void deleteDirectory(File d) {
        String[] list = d.list();
        if (list == null) {
            list = new String[0];
        }
        for (int i = 0; i < list.length; i++) {
            String s = list[i];
            File f = new File(d, s);
            if (f.isDirectory()) {
                deleteDirectory(f);
            } else {
                delete(f);
            }
        }
        delete(d);
    }

    public static void delete(File f) {
        if (!f.delete()) {
            if (isWindows()) {
                System.gc();
            }
            try {
                Thread.sleep(1);
            } catch (InterruptedException ex) {
                // Ignore Exception
            }
            f.delete();
        }
    }
}


