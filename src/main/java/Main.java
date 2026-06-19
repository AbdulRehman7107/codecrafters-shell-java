import java.util.*;
import java.io.File;
import java.io.PrintStream;

public class Main {
    public static void main(String[] args) throws Exception {
        // TODO: Uncomment the code below to pass the first stage
    	Scanner sc = new Scanner(System.in);
    	List<String> builtins = List.of("echo", "exit", "type", "pwd", "cd");
    	
    	while(true) {
    		System.out.print("$ ");
    		System.out.flush();
            
    		String string = sc.nextLine().trim();
    		if(string.isEmpty()) {
    			continue;
    		}
    		
    		//Parse the input string into arguments properly handling single quotes
    		List<String> argsList = parseArgument(string);
    		if(argsList.isEmpty()) {
    			continue;
    		}
    		
    		// Check for stdout redirection operators (> or 1>)
    		String outputFile = null;
    		int redirectIndex = -1;
    		for (int i = 0; i < argsList.size(); i++) {
    			if (argsList.get(i).equals(">") || argsList.get(i).equals("1>")) {
    				redirectIndex = i;
    				break;
    			}
    		}
    		
    		// If a redirection operator is found, extract the file and strip them from args
    		if (redirectIndex != -1 && redirectIndex + 1 < argsList.size()) {
    			outputFile = argsList.get(redirectIndex + 1);
    			// Remove both the operator and the filename from our execution arguments
    			argsList.remove(redirectIndex + 1);
    			argsList.remove(redirectIndex);
    		}
    		
    		String command = argsList.get(0);
    		
    		// Save the original stdout stream to restore it later for builtins
    		PrintStream originalOut = System.out;
    		PrintStream fileOut = null;
    		if (outputFile != null) {
    			File file = new File(outputFile);
    			// Ensure parent directories exist if specified
    			if (file.getParentFile() != null) {
    				file.getParentFile().mkdirs();
    			}
    			fileOut = new PrintStream(file);
    			System.setOut(fileOut);
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
	            	} 
	            	
	            	else if(pathArg.equals("~")) {
	            		String homeDir = System.getenv("HOME");
	            		newDir = new File(homeDir);
	            	}
	            	
	            	else {
	            		newDir = new File(System.getProperty("user.dir"),pathArg);
	            		
	            	}
	            	
	            	newDir = newDir.getAbsoluteFile();
	            	
	            	try {
	            		newDir = newDir.getCanonicalFile();
	            	}catch(Exception a) {
	            		
	            	}
	            	
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
	            
	            //handle external programs
	            else {
	            	String executablePath = getPath(command);
	            	
	            	if(executablePath != null) {
	            		ProcessBuilder pb =new ProcessBuilder(argsList);
	            		pb.directory(new File(System.getProperty("user.dir")));
	            		
	            		if (outputFile != null) {
	            			// If redirecting, dump standard output to the file but leave standard error on the console
	            			pb.redirectOutput(new File(outputFile));
	            			pb.redirectError(ProcessBuilder.Redirect.INHERIT);
	            		} else {
	            			pb.inheritIO();
	            		}
	            		
	            		Process process = pb.start();
	            		process.waitFor();
	            	} else {
	            		// Print command error tracking to stderr to keep it isolated from file routes
	            		System.err.println(command+": command not found");
	            	}
	            }
    		} finally {
    			// Always clean up and restore System.out back to normal terminal output
    			if (fileOut != null) {
    				fileOut.close();
    				System.setOut(originalOut);
    			}
    		}
    	}  
    }
    
    //helper method to parse strings supporting single quotes and concatenation
    private static List<String> parseArgument(String string){
    	List<String> list = new ArrayList<>();
    	StringBuilder currentArg = new StringBuilder();
    	boolean inSingleQuotes = false;
    	boolean inDoubleQuotes = false;
    	boolean hasContent = false; //tracks if we have started building an argument
    	
    	for(int i=0; i< string.length();i++) {
    		char c = string.charAt(i);
    		
    		if(inSingleQuotes) {
    			if(c== '\'') {
    				inSingleQuotes = false; //close quote
    			}
    			else {
    				currentArg.append(c);
    			}
    			hasContent = true;
    		}
    		else if(inDoubleQuotes) {
    			if(c == '"') {
    				inDoubleQuotes = false;//close double
    			}
    			else if(c == '\\' && i + 1 < string.length()) {
    				char next = string.charAt(i + 1);
    				if(next == '"' || next == '\\' || next == '$' || next == '`') {
    					currentArg.append(next);
    					i++; // skip the escaped character
    				} else {
    					currentArg.append(c); // treat backslash literally
    				}
    			}
    			else {
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
    			}
    			else if(c == '\'') {
    				inSingleQuotes = true;//open quote
    				hasContent=true;
    			}
    			else if(c== '"') {
    				inDoubleQuotes = true;
    				hasContent= true;
    			}
    			else if(c == ' ') {
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