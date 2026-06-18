import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws Exception {
        // TODO: Uncomment the code below to pass the first stage
    	Scanner sc = new Scanner(System.in);
    	
    	while(true) {
    		System.out.print("$ ");
    		System.out.flush();
            String string = sc.nextLine();
            if(string.equals("exit")) {
            	break;
            } else if(string.startsWith("echo")) {
            	String result = string.substring(5);
            	System.out.println(result);
            } else {
            	System.out.println(string + ": command not found");
            }
    	}
        
    }
}
