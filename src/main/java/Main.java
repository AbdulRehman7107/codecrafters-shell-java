import java.util.*;
import java.io.File;

public class Main {
    public static void main(String[] args) throws Exception {
        // TODO: Uncomment the code below to pass the first stage
    	Scanner sc = new Scanner(System.in);
    	List<String> builtins = List.of("echo", "exit", "type");
    	
    	while(true) {
    		System.out.print("$ ");
    		System.out.flush();
            
    		String string = sc.nextLine().trim();
    		if(string.isEmpty()) {
    			continue;
    		}
    		
            if(string.equals("exit")) {
            	break;
            }
            
            else if(string.startsWith("echo")) {
            	String result = string.substring(5);
            	System.out.println(result);
            }
            
            else if(string.startsWith("type")) {
            	String arg = string.substring(5).trim();
            	
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
            else {
            	String[] inputParts = string.split(" ");
            	String command = inputParts[0];
            	
            	String executablePath = getPath(command);
            	
            	if(executablePath != null) {
            		List<String> commandList = new ArrayList<>();
            		commandList.add(command);
            		
            		for(int i=0; i<inputParts.length; i++) {
            			commandList.add(inputParts[i]);
            		}
            		
            		ProcessBuilder pb =new ProcessBuilder(commandList);
            		pb.inheritIO();
            		
            		Process process = pb.start();
            		process.waitFor();
            	} else {
            		System.out.println(string+": command not found");
            	}
            }
    	}  
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
