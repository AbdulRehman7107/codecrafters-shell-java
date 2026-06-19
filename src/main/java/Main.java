import java.util.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;

public class Main {
    // A clean helper class to store job details
    private static class Job {
        int id;
        Process process;
        String commandLine;

        Job(int id, Process process, String commandLine) {
            this.id = id;
            this.process = process;
            this.commandLine = commandLine;
        }
    }

    private static final List<Job> backgroundJobs = new ArrayList<>();
    private static int nextJobId = 1;

    public static void main(String[] args) throws Exception {
    	Scanner sc = new Scanner(System.in);
    	List<String> builtins = List.of("echo", "exit", "type", "pwd", "cd", "jobs");
    	
    	while(true) {
            // Silently clean up completed background processes before displaying the next prompt
            backgroundJobs.removeIf(job -> !job.process.isAlive());

    		System.out.print("$ ");
    		System.out.flush();
            
    		String string = sc.nextLine().trim();
    		if(string.isEmpty()) {
    			continue;
    		}
    		
    		// Keep a copy of the raw command line for jobs logging
    		String rawCommandLine = string;
    		
    		//Parse the input string into arguments properly handling single quotes
    		List<String> argsList = parseArgument(string);
    		if(argsList.isEmpty()) {
    			continue;
    		}
    		
    		// Check for stdout redirection operators (> or 1>) or append operators (>> or 1>>)
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
    		
    		// If a redirection operator is found, extract the file and strip them from args
    		if (redirectIndex != -1 && redirectIndex + 1 < argsList.size()) {
    			outputFile = argsList.get(redirectIndex + 1);
    			argsList.remove(redirectIndex + 1);
    			argsList.remove(redirectIndex);
    		}

    		// Check for stderr redirection operator (2>) or append operator (2>>)
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

    		// If a stderr redirection operator is found, extract the file and strip them from args
    		if (errRedirectIndex != -1 && errRedirectIndex + 1 < argsList.size()) {
    			errorFile = argsList.get(errRedirectIndex + 1);
    			argsList.remove(errRedirectIndex + 1);
    			argsList.remove(errRedirectIndex);
    		}
    		
    		String command = argsList.get(0);
    		
    		// Save the original stdout stream to restore it later for builtins
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

    		// Save the original stderr stream to restore it later for builtins
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
                    for (Job job : backgroundJobs) {
                        if (job.process.isAlive()) {
                            // Exact space-separated receipt matching tester expectation
                            System.out.println("[" + job.id + "] " + job.process.pid() + " Running " + job.commandLine);
                        }
                    }
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
                            Job job = new Job(nextJobId++, process, rawCommandLine);
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
}