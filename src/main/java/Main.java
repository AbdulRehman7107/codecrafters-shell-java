import java.util.*;
import java.io.File;

public class Main {
    public static void main(String[] args) throws Exception {
    	Scanner sc = new Scanner(System.in);
    	List<String> builtins = List.of("echo", "exit", "type", "pwd", "cd");
    	
    	while(true) {
    		System.out.print("$ ");
    		System.out.flush();
            
    		String string = sc.nextLine().trim();
    		if(string.isEmpty()) {
    			continue;
    		}
    		
    		// Parse the input string into arguments properly handling quotes and backslashes
    		List<String> argsList = parseArgument(string);
    		if(argsList.isEmpty()) {
    			continue;
    		}
    		
    		String command = argsList.get(0);
    		
    		// 1. Handle exit command (Using parsed variable)
            if(command.equals("exit")) {
            	break;
            }
            
            // 2. Handle echo command (Using parsed variable)
            else if(command.equals("echo")) {
            	StringBuilder sb = new StringBuilder();
            	for(int i = 1; i < argsList.size(); i++) {
            		sb.append(argsList.get(i));
            		if(i < argsList.size() - 1) {
            			sb.append(" ");
            		}
            	}
            	System.out.println(sb.toString());
            }
            
            // 3. Handle pwd command (Using parsed variable)
            else if (command.equals("pwd")) {
                String currentDir = System.getProperty("user.dir");
                System.out.println(currentDir);
            }
            
            // 4. Handle cd command (Using parsed variable)
            else if(command.equals("cd")) {
            	String pathArg = argsList.size() > 1 ? argsList.get(1) : "";
            	File newDir;
            	
            	if(pathArg.startsWith("/")) {
            		newDir = new File(pathArg);
            	} 
            	else if(pathArg.equals("~")) {
            		String homeDir = System.getenv("HOME");
            		newDir = new File(homeDir);
            	}
            	else {
            		newDir = new File(System.getProperty("user.dir"), pathArg);
            	}
            	
            	newDir = newDir.getAbsoluteFile();
            	
            	try {
            		newDir = newDir.getCanonicalFile();
            	} catch(Exception a) {}
            	
            	if(newDir.exists() && newDir.isDirectory()) {
            		System.setProperty("user.dir", newDir.getAbsolutePath());
            	} else {
            		System.out.println("cd: " + pathArg + ": No such file or directory");
            		System.out.flush();
            	}
            }
            
            // 5. Handle type command (Using parsed variable)
            else if(command.equals("type")) {
            	String arg = argsList.size() > 1 ? argsList.get(1) : "";
            	
            	if(builtins.contains(arg)) {
            		System.out.println(arg + " is a shell builtin");
            	} else {
            		String pathRoute = getPath(arg);
            		if(pathRoute != null) {
            			System.out.println(arg + " is " + pathRoute);
            		} else {
                		System.out.println(arg + ": not found");
                	}
            	}
            }
            
            // 6. Handle external programs
            else {
            	String executablePath = getPath(command);
            	if(executablePath != null) {
            		ProcessBuilder pb = new ProcessBuilder(argsList);
            		pb.directory(new File(System.getProperty("user.dir")));
            		pb.inheritIO();
            		
            		Process process = pb.start();
            		process.waitFor();
            	} else {
            		System.out.println(command + ": command not found");
            	}
            }
    	}  
    }
    
    // Updated helper method to support backslash escaping outside quotes
    private static List<String> parseArgument(String string) {
    	List<String> list = new ArrayList<>();
    	StringBuilder currentArg = new StringBuilder();
    	boolean inSingleQuotes = false;
    	boolean inDoubleQuotes = false;
    	boolean hasContent = false; 
    	
    	for(int i = 0; i < string.length(); i++) {
    		char c = string.charAt(i);
    		
    		if(inSingleQuotes) {
    			if(c == '\'') {
    				inSingleQuotes = false; 
    			} else {
    				currentArg.append(c);
    			}
    			hasContent = true;
    		}
    		else if(inDoubleQuotes) {
    			if(c == '"') {
    				inDoubleQuotes = false;
    			} else {
    				currentArg.append(c);
    			}
    			hasContent = true;
    		}
    		else {
    			// NEW: Handle backslash escaping outside of quotes
    			if(c == '\\') {
    				if(i + 1 < string.length()) {
    					currentArg.append(string.charAt(i + 1)); // Append the escaped next char literally
    					i++; // Skip the next character as it's processed
    					hasContent = true;
    				}
    			}
    			else if(c == '\'') {
    				inSingleQuotes = true;
    				hasContent = true;
    			}
    			else if(c == '"') {
    				inDoubleQuotes = true;
    				hasContent = true;
    			}
    			else if(c == ' ') {
    				if(hasContent) {
    					list.add(currentArg.toString());
    					currentArg.setLength(0);
    					hasContent = false;
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