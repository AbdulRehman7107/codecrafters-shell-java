import java.util.*;

public class Main {
    public static void main(String[] args) throws Exception {
        // TODO: Uncomment the code below to pass the first stage
    	Scanner sc = new Scanner(System.in);
    	List<String> builtins = List.of("echo", "exit", "type");
    	while(true) {
    		System.out.print("$ ");
    		System.out.flush();
            String string = sc.nextLine();
            if(string.equals("exit")) {
            	break;
            } else if(string.startsWith("echo")) {
            	String result = string.substring(5);
            	System.out.println(result);
            } 
            else if(string.startsWith("type")) {
            	String arg = string.substring(5).trim();
            	
            	if(builtins.contains(arg)) {
            		System.out.println(arg+" is a shell builtin");
            	} else {
            		System.out.println(arg+": not found");
            	}
            }
            
            else {
            	System.out.println(string + ": command not found");
            }
    	}
        
    }
}
