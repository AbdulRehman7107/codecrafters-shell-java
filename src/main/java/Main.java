import java.util.*;
import java.io.File;

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
    		if(string.isEmpty()) {
    			continue;
    		}
    		
    		String command = argsList.get(0);
    		
    		//handle exit command
            if(string.equals("exit")) {
            	break;
            }
            
            //handle echo command
            else if(string.startsWith("echo")) {
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
            else if (string.equals("pwd")) {
               
                String currentDir = System.getProperty("user.dir");
                System.out.println(currentDir);
            }
            
            //handle cd command
            else if(string.startsWith("cd")) {
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
            		System.out.println("cd: "+pathArg+": No such file or directory");
            		System.out.flush();
            	}
            }
            
            // handle type command
            else if(string.startsWith("type")) {
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
//            	String[] inputParts = string.split(" ");
//            	String command = inputParts[0];
            	
            	String executablePath = getPath(command);
            	
            	if(executablePath != null) {
//            		List<String> commandList = new ArrayList<>();
//            		commandList.add(command);
//            		
//            		for(int i=1; i<inputParts.length; i++) {
//            			commandList.add(inputParts[i]);
//            		}
            		
            		ProcessBuilder pb =new ProcessBuilder(argsList);
            		pb.directory(new File(System.getProperty("user.dir")));
            		pb.inheritIO();
            		
            		Process process = pb.start();
            		process.waitFor();
            	} else {
            		System.out.println(string+": command not found");
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
    			}else {
    				currentArg.append(c);
    			}
    			hasContent = true;
    		}
    		else {
    			if(c == '\'') {
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
