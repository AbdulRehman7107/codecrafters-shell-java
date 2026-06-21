import java.util.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;

public class Main {
    //helper class to store job detail
	private static class Job {

	    int id;
	    Process process;
	    String commandLine;
	    boolean doneDisplayed = false;

	    Job(int id, Process process, String commandLine) {
	        this.id = id;
	        this.process = process;
	        this.commandLine = commandLine;
	    }
	}
	
	private static boolean isBuiltin(String cmd) {
	    return cmd.equals("echo")
	        || cmd.equals("type")
	        || cmd.equals("pwd")
	        || cmd.equals("cd")
	        || cmd.equals("exit")
	        || cmd.equals("jobs");
	}

    private static final List<Job> backgroundJobs = new ArrayList<>();
//    private static int nextJobId = 1;

    public static void main(String[] args) throws Exception {
    	Scanner sc = new Scanner(System.in);
    	List<String> builtins = List.of("echo", "exit", "type", "pwd", "cd", "jobs");
    	
    	while(true) {
            
    		reapCompletedJobs();

    		System.out.print("$ ");
    		System.out.flush();
            
    		String string = sc.nextLine().trim();
    		if(string.isEmpty()) {
    			continue;
    		}
    		
    		// copy of the raw command line for jobs logging
    		String rawCommandLine = string;
    		
    		//parse the input string into arguments properly handling single quotes
    		List<String> argsList = parseArgument(string);
    		if (string.contains("|")) {

    		    String[] parts = string.split("\\|");

    		    if (parts.length == 2) {
    		        executePipelineWithBuiltins(
    		            string,
    		            builtins
    		        );
    		    } else {
    		        executePipeline(string);
    		    }

    		    continue;
    		}
    		if(argsList.isEmpty()) {
    			continue;
    		}
    		
    		//check for stdout redirection operators (> or 1>) or append operators (>> or 1>>)
    		String outputFile = null;
    		boolean appendMode = false;
    		int redirectIndex = -1;
    		for (int i = 0; i < argsList.size(); i++) {
    			if (argsList.get(i).equals(">>") || argsList.get(i).equals("1>>")) {
    				redirectIndex = i;
    				appendMode = true;
    				break;
    			} else if (argsList.get(i).equals(">") || argsList.get(i).equals("1>")) {
    				redirectIndex = i;
    				appendMode = false;
    				break;
    			}
    		}
    		
    		//if a redirection operator is found, extract the file and strip them from args
    		if (redirectIndex != -1 && redirectIndex + 1 < argsList.size()) {
    			outputFile = argsList.get(redirectIndex + 1);
    			argsList.remove(redirectIndex + 1);
    			argsList.remove(redirectIndex);
    		}

    		//check for stderr redirection operator (2>) or append operator (2>>)
    		String errorFile = null;
    		boolean errAppendMode = false;
    		int errRedirectIndex = -1;
    		for (int i = 0; i < argsList.size(); i++) {
    			if (argsList.get(i).equals("2>>")) {
    				errRedirectIndex = i;
    				errAppendMode = true;
    				break;
    			} else if (argsList.get(i).equals("2>")) {
    				errRedirectIndex = i;
    				errAppendMode = false;
    				break;
    			}
    		}

    		//if a stderr redirection operator is found, extract the file and strip them from args
    		if (errRedirectIndex != -1 && errRedirectIndex + 1 < argsList.size()) {
    			errorFile = argsList.get(errRedirectIndex + 1);
    			argsList.remove(errRedirectIndex + 1);
    			argsList.remove(errRedirectIndex);
    		}
    		
    		String command = argsList.get(0);
    		
    		//save the original stdout stream to restore it later for builtins
    		PrintStream originalOut = System.out;
    		PrintStream fileOut = null;
    		if (outputFile != null) {
    			File file = new File(outputFile);
    			if (file.getParentFile() != null) {
    				file.getParentFile().mkdirs();
    			}
    			fileOut = new PrintStream(new FileOutputStream(file, appendMode));
    			System.setOut(fileOut);
    		}

    		//save the original stderr stream to restore it later for builtins
    		PrintStream originalErr = System.err;
    		PrintStream fileErr = null;
    		if (errorFile != null) {
    			File file = new File(errorFile);
    			if (file.getParentFile() != null) {
    				file.getParentFile().mkdirs();
    			}
    			fileErr = new PrintStream(new FileOutputStream(file, errAppendMode));
    			System.setErr(fileErr);
    		}
    		
    		try {
	    		//handle exit command
	            if(command.equals("exit")) {
	            	break;
	            }
	            
	            //handle echo command
	            else if(command.equals("echo")) {
	            	StringBuilder sb = new StringBuilder();
	            	for(int i=1; i< argsList.size();i++) {
	            		sb.append(argsList.get(i));
	            		if(i< argsList.size()-1) {
	            			sb.append(" ");
	            		}
	            	}
	            	System.out.println(sb.toString());
	            }
	            
	            //handle pwd command
	            else if (command.equals("pwd")) {
	                String currentDir = System.getProperty("user.dir");
	                System.out.println(currentDir);
	            }
	            
	            //handle cd command
	            else if(command.equals("cd")) {
	            	String pathArg = argsList.size() > 1? argsList.get(1) : "";
	            	File newDir;
	            	if(pathArg.startsWith("/")) {
	            		newDir = new File(pathArg);
	            	} else if(pathArg.equals("~")) {
	            		String homeDir = System.getenv("HOME");
	            		newDir = new File(homeDir);
	            	} else {
	            		newDir = new File(System.getProperty("user.dir"),pathArg);
	            	}
	            	newDir = newDir.getAbsoluteFile();
	            	try {
	            		newDir = newDir.getCanonicalFile();
	            	}catch(Exception a) {}
	            	
	            	if(newDir.exists() && newDir.isDirectory()) {
	            		System.setProperty("user.dir", newDir.getAbsolutePath());
	            	} else {
	            		System.err.println("cd: "+pathArg+": No such file or directory");
	            		System.err.flush();
	            	}
	            }
	            
	            // handle type command
	            else if(command.equals("type")) {
	            	String arg = argsList.size()> 1? argsList.get(1):"";
	            	if(builtins.contains(arg)) {
	            		System.out.println(arg+" is a shell builtin");
	            	} else {
	            		String pathRoute = getPath(arg);
	            		if(pathRoute != null) {
	            			System.out.println(arg+" is "+pathRoute);
	            		}else {
	                		System.out.println(arg+": not found");
	                	}
	            	}
	            }

	            // handle jobs command
	            else if(command.equals("jobs")) {

	                int size = backgroundJobs.size();

	                for (int i = 0; i < size; i++) {

	                    Job job = backgroundJobs.get(i);

	                    char marker = ' ';

	                    if (size == 1) {
	                        marker = '+';
	                    } else if (i == size - 1) {
	                        marker = '+';
	                    } else if (i == size - 2) {
	                        marker = '-';
	                    }

	                    if (job.process.isAlive()) {

	                        System.out.printf(
	                            "[%d]%c  Running                 %s%n",
	                            job.id,
	                            marker,
	                            job.commandLine
	                        );

	                    } else {

	                        String cmd = job.commandLine;

	                        if (cmd.endsWith(" &")) {
	                            cmd = cmd.substring(0, cmd.length() - 2);
	                        }

	                        System.out.printf(
	                            "[%d]%c  Done                    %s%n",
	                            job.id,
	                            marker,
	                            cmd
	                        );

	                        job.doneDisplayed = true;
	                    }
	                }

	                backgroundJobs.removeIf(job -> job.doneDisplayed);
	            }
	            
	            //handle external programs
	            else {
	            	String executablePath = getPath(command);
	            	
	            	if(executablePath != null) {
	            		boolean background = false;
	            		if (argsList.size() > 1 && argsList.get(argsList.size() - 1).equals("&")) {
	            			background = true;
	            			argsList.remove(argsList.size() - 1);
	            		}

	            		ProcessBuilder pb = new ProcessBuilder(argsList);
	            		pb.directory(new File(System.getProperty("user.dir")));
	            		
	            		if (outputFile != null) {
	            			if (appendMode) {
	            				pb.redirectOutput(ProcessBuilder.Redirect.appendTo(new File(outputFile)));
	            			} else {
	            				pb.redirectOutput(new File(outputFile));
	            			}
	            		} else {
	            			pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
	            		}

	            		if (errorFile != null) {
	            			if (errAppendMode) {
	            				pb.redirectError(ProcessBuilder.Redirect.appendTo(new File(errorFile)));
	            			} else {
	            				pb.redirectError(new File(errorFile));
	            			}
	            		} else {
	            			pb.redirectError(ProcessBuilder.Redirect.INHERIT);
	            		}
	            		
	            		Process process = pb.start();
	            		if (!background) {
	            			process.waitFor();
	            		} else {
	            			int jobId = getNextJobId();

	            			Job job = new Job(jobId, process, rawCommandLine);

	            			backgroundJobs.add(job);

	            			System.out.println("[" + job.id + "] " + process.pid());
                        }
	            	} else {
	            		System.err.println(command+": command not found");
	            	}
	            }
    		} finally {
    			if (fileOut != null) {
    				fileOut.close();
    				System.setOut(originalOut);
    			}
    			if (fileErr != null) {
    				fileErr.close();
    				System.setErr(originalErr);
    			}
    		}
    	}  
    }
    
    private static void executePipelineWithBuiltins(
            String commandLine,
            List<String> builtins) throws Exception {

        String[] parts = commandLine.split("\\|", 2);

        String leftPart = parts[0].trim();
        String rightPart = parts[1].trim();

        List<String> leftArgs = parseArgument(leftPart);
        List<String> rightArgs = parseArgument(rightPart);

        String leftCmd = leftArgs.get(0);
        String rightCmd = rightArgs.get(0);

        //case 1
        if (isBuiltin(leftCmd) && !isBuiltin(rightCmd)) {

            StringBuilder builtinOutput = new StringBuilder();

            if (leftCmd.equals("echo")) {

                for (int i = 1; i < leftArgs.size(); i++) {

                    if (i > 1) {
                        builtinOutput.append(" ");
                    }

                    builtinOutput.append(leftArgs.get(i));
                }

                builtinOutput.append("\n");
            }

            ProcessBuilder pb =
                new ProcessBuilder(rightArgs);

            pb.directory(
                new File(System.getProperty("user.dir"))
            );

            Process process = pb.start();

            process.getOutputStream()
                   .write(builtinOutput.toString().getBytes());

            process.getOutputStream().close();

            process.getInputStream()
                   .transferTo(System.out);

            process.waitFor();

            return;
        }

      //case 2
        if (!isBuiltin(leftCmd) && isBuiltin(rightCmd)) {

            ProcessBuilder pb =
                new ProcessBuilder(leftArgs);

            pb.directory(
                new File(System.getProperty("user.dir"))
            );

            Process process = pb.start();

            if (rightCmd.equals("type")) {

                String arg =
                    rightArgs.size() > 1
                        ? rightArgs.get(1)
                        : "";

                if (builtins.contains(arg)) {

                    System.out.println(
                        arg + " is a shell builtin"
                    );

                } else {

                    String path = getPath(arg);

                    if (path != null) {

                        System.out.println(
                            arg + " is " + path
                        );

                    } else {

                        System.out.println(
                            arg + ": not found"
                        );
                    }
                }
            }

            process.destroy();

            return;
        }

        // case 3
        executePipeline(commandLine);
    }
    
    private static void executePipeline(String commandLine) throws Exception {

        String[] parts = commandLine.split("\\|");

        int n = parts.length;

        List<Process> processes = new ArrayList<>();

        for (int i = 0; i < n; i++) {

            List<String> args =
                parseArgument(parts[i].trim());

            ProcessBuilder pb =
                new ProcessBuilder(args);

            pb.directory(
                new File(System.getProperty("user.dir"))
            );

            processes.add(pb.start());
        }

        List<Thread> pipeThreads = new ArrayList<>();

        for (int i = 0; i < n - 1; i++) {

            Process current = processes.get(i);
            Process next = processes.get(i + 1);

            Thread t = new Thread(() -> {

                try (
                    var in = current.getInputStream();
                    var out = next.getOutputStream()
                ) {

                    byte[] buffer = new byte[8192];
                    int bytesRead;

                    while ((bytesRead = in.read(buffer)) != -1) {

                        out.write(buffer, 0, bytesRead);
                        out.flush();
                    }

                    out.close();

                } catch (Exception ignored) {
                }
            });

            t.start();
            pipeThreads.add(t);
        }

        Thread stdoutThread = new Thread(() -> {

            try {

                Process last =
                    processes.get(processes.size() - 1);

                byte[] buffer = new byte[8192];
                int bytesRead;

                var in = last.getInputStream();

                while ((bytesRead = in.read(buffer)) != -1) {

                    System.out.write(
                        buffer,
                        0,
                        bytesRead
                    );

                    System.out.flush();
                }

            } catch (Exception ignored) {
            }
        });

        stdoutThread.start();

        List<Thread> stderrThreads = new ArrayList<>();

        for (Process p : processes) {

            Thread t = new Thread(() -> {

                try {

                    p.getErrorStream()
                     .transferTo(System.err);

                } catch (Exception ignored) {
                }
            });

            t.start();
            stderrThreads.add(t);
        }

        Process last =
            processes.get(processes.size() - 1);

        last.waitFor();

        stdoutThread.join();

        for (Thread t : pipeThreads) {
            t.join();
        }

        for (Process p : processes) {

            if (p.isAlive()) {
                p.destroy();
            }
        }
    }
    
    private static List<String> parseArgument(String string){
    	List<String> list = new ArrayList<>();
    	StringBuilder currentArg = new StringBuilder();
    	boolean inSingleQuotes = false;
    	boolean inDoubleQuotes = false;
    	boolean hasContent = false;
    	
    	for(int i=0; i< string.length();i++) {
    		char c = string.charAt(i);
    		
    		if(inSingleQuotes) {
    			if(c== '\'') {
    				inSingleQuotes = false;
    			} else {
    				currentArg.append(c);
    			}
    			hasContent = true;
    		}
    		else if(inDoubleQuotes) {
    			if(c == '"') {
    				inDoubleQuotes = false;
    			} else if(c == '\\' && i + 1 < string.length()) {
    				char next = string.charAt(i + 1);
    				if(next == '"' || next == '\\' || next == '$' || next == '`') {
    					currentArg.append(next);
    					i++;
    				} else {
    					currentArg.append(c);
    				}
    			} else {
    				currentArg.append(c);
    			}
    			hasContent = true;
    		}
    		else {
    			if(c == '\\') {
    				if(i + 1 < string.length()) {
    					currentArg.append(string.charAt(i + 1));
    					i++;
    					hasContent = true;
    				}
    			} else if(c == '\'') {
    				inSingleQuotes = true;
    				hasContent=true;
    			} else if(c== '"') {
    				inDoubleQuotes = true;
    				hasContent= true;
    			} else if(c == ' ') {
    				if(hasContent) {
    					list.add(currentArg.toString());
    					currentArg.setLength(0);
    					hasContent= false;
    				}
    			} else {
    				currentArg.append(c);
    				hasContent = true;
    			}
    		}
    	}
    	
    	if(hasContent) {
    		list.add(currentArg.toString());
    	}
    	return list;
    }
    
    private static String getPath(String command) {
    	String pathEnv = System.getenv("PATH");
    	if(pathEnv == null) {
    		return null;
    	}
    	String[] directories = pathEnv.split(File.pathSeparator);
    	for(String directory : directories) {
    		File file = new File(directory, command);
    		if(file.exists() && file.canExecute()) {
    			return file.getAbsolutePath();
    		}
    	}
    	return null;
    }
    
    private static int getNextJobId() {

        if (backgroundJobs.isEmpty()) {
            return 1;
        }

        int maxId = 0;

        for (Job job : backgroundJobs) {
            maxId = Math.max(maxId, job.id);
        }

        return maxId + 1;
    }
    
    private static void reapCompletedJobs() {

        int size = backgroundJobs.size();

        Iterator<Job> it = backgroundJobs.iterator();
        int index = 0;

        while (it.hasNext()) {

            Job job = it.next();

            if (!job.process.isAlive()) {

                char marker = ' ';

                if (size == 1) {
                    marker = '+';
                } else if (index == size - 1) {
                    marker = '+';
                } else if (index == size - 2) {
                    marker = '-';
                }

                String cmd = job.commandLine;

                if (cmd.endsWith(" &")) {
                    cmd = cmd.substring(0, cmd.length() - 2);
                }

                System.out.printf(
                    "[%d]%c  Done                    %s%n",
                    job.id,
                    marker,
                    cmd
                );

                it.remove();
            }

            index++;
        }
    }
}
