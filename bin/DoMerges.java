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
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.*;


/* dkulp - Stupid little program I use to help merge changes from 
   trunk to the fixes branches.   It requires the svnmerge.py be 
   available on the path.   Grab the latest from:
   http://svn.collab.net/repos/svn/trunk/contrib/client-side/svnmerge/
   (of course, that then requries python installed and whatever else svnmerge.py
   needs.)   It also requires the command line version of svn.

   Basically, svnmerge.py does all the work, but this little wrapper 
   thing will display the commit logs, prompt if you want to merge/block/ignore
   each commit, prompt for commit (so you can resolve any conflicts first), 
   etc....

   Yes - doing this in python itself (or perl or even bash itself or ruby or ...) 
   would probably be better.  However, I'd then need to spend time 
   learning python/ruby/etc... that I just don't have time to do right now.
   What is more productive: Taking 30 minutes to bang this out in Java or
   spending a couple days learning another language that would allow me to
   bang it out in 15 minutes?
*/

public class DoMerges {
    public static boolean auto = false;

    static void doCommit() throws Exception {
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
            return;
        }
        Process p = Runtime.getRuntime().exec(new String[] {"svn", "resolved", "."});
        if (p.waitFor() != 0) {
            Thread.sleep(10);
        }
        p = Runtime.getRuntime().exec(new String[] {"svn", "commit", "-F", "svnmerge-commit-message.txt"});
        BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
        String line = reader.readLine();
        while (line != null) {
            System.out.println(line);
            line = reader.readLine();
        }
        if (p.waitFor() != 0) {
            System.out.println("ERROR!");
            reader = new BufferedReader(new InputStreamReader(p.getErrorStream()));
            line = reader.readLine();
            while (line != null) {
                System.out.println(line);
                line = reader.readLine();
            }
            System.exit(1);
        }
    }   

    public static void main (String args[]) throws Exception {
        if (args.length > 0 && "-auto".equals(args[0])) { 
            auto = true;
        }

        System.out.println("Updating directory");

        Process p = Runtime.getRuntime().exec(new String[] {"svn", "up", "-r", "head", "."});
        BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
        String line = reader.readLine();
        while (line != null) {
            System.out.println(line);
            line = reader.readLine();
        }
        p.waitFor();


        p = Runtime.getRuntime().exec(getCommandLine(new String[] {"svnmerge.py", "avail"}));

        reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
        line = reader.readLine();
        List<String> verList = new ArrayList<String>();
        while (line != null) {
            String vers[] = line.split(",");
            for (String s : vers) {
                if (s.indexOf("-") != -1) {
                    String s1 = s.substring(0, s.indexOf("-"));
                    String s2 = s.substring(s.indexOf("-") + 1);
                    int i1 = Integer.parseInt(s1);
                    int i2 = Integer.parseInt(s2);
                    for (int x = i1; x <= i2; x++) {
                        verList.add(Integer.toString(x));
                    }                
                } else {
                    verList.add(s);
                } 
            }
            line = reader.readLine();
        }
        p.waitFor();
        System.out.println("Merging versions (" + verList.size() + "): " + verList);




        String root = null;

        p = Runtime.getRuntime().exec(new String[] {"svn", "info"});
        reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
        line = reader.readLine();
        while (line != null) {
            if (line.startsWith("Repository Root: ")) {
                root = line.substring("Repository Root: ".length()).trim();
            }
            line = reader.readLine();
        }
        System.out.println("Root: " + root);
        p.waitFor();


        int count = 1;
        for (String ver : verList) {
            System.out.println("Merging: " + ver + " (" + (count++) + "/" + verList.size() + ")");
            p = Runtime.getRuntime().exec(new String[] {"svn", "log", "-r" , ver, root});
            reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            line = reader.readLine();
            while (line != null) {
                System.out.println(line);
                line = reader.readLine();
            }
            p.waitFor();

            while (System.in.available() > 0) {
                System.in.read();
            }
            char c = auto ? 'M' : 0;
            while (c != 'M'
                   && c != 'B'
                   && c != 'I') {
                System.out.print("[M]erge, [B]lock, or [I]gnore? ");
                int i = System.in.read();
                c = Character.toUpperCase((char)i);
            }

            switch (c) {
            case 'M':
                p = Runtime.getRuntime().exec(getCommandLine(new String[] {"svnmerge.py", "merge", "-r", ver}));
                reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
                line = reader.readLine();
                while (line != null) {
                    System.out.println(line);
                    line = reader.readLine();
                }
                if (p.waitFor() != 0) {
                    System.out.println("ERROR!");
                    reader = new BufferedReader(new InputStreamReader(p.getErrorStream()));
                    line = reader.readLine();
                    while (line != null) {
                        System.out.println(line);
                        line = reader.readLine();
                    }
                    System.exit(1);
                }

                doCommit();
                break;
            case 'B':
                p = Runtime.getRuntime().exec(getCommandLine(new String[] {"svnmerge.py", "block", "-r", ver}));
                reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
                line = reader.readLine();
                while (line != null) {
                    System.out.println(line);
                    line = reader.readLine();
                }
                if (p.waitFor() != 0) {
                    System.out.println("ERROR!");
                    reader = new BufferedReader(new InputStreamReader(p.getErrorStream()));
                    line = reader.readLine();
                    while (line != null) {
                        System.out.println(line);
                        line = reader.readLine();
                    }
                    System.exit(1);
                }
                doCommit();
                break;
            case 'I':
                System.out.println("Ignoring");
                break;
            }
        }
    }

    private static String[] getCommandLine(String[] args) {
        List<String> argLine = new ArrayList<String>();
        if (isWindows()) {
            argLine.add("cmd.exe");
            argLine.add("/c");
        }

        argLine.addAll(Arrays.asList(args));
        System.out.println("Running " + argLine + "...");
        return argLine.toArray(new String[argLine.size()]);
    }

    private static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().indexOf("windows") != -1;
    }
}
