import java.io.IOException;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.nio.file.Files;
import java.io.FileWriter;
import java.util.*;

public class Duke {
    //set a horizontal line
    public static StringBuilder horizontalLine = new StringBuilder("   ").append("\u02DC".repeat(80)).append("\n");

    public static HashMap<String, Message> availableMessage = new HashMap<>() {{
        put("done", Message.DONE);
        put("delete", Message.DELETE);
        put("todo", Message.TODO);
        put("deadline", Message.DEADLINE);
        put("event", Message.EVENT);
    }};

    public static String taskData = "./data/duke.txt";

    //to add horizontal line around the message and print it out
    public static void typeSetting(String s) {
        System.out.println(horizontalLine + s + "\n" + horizontalLine);
    }

    //get a list which listing all the tasks recorded
    public static String gettingList(List<Task> list) {
        int index = list.size();
        StringBuilder li = new StringBuilder("    \uD83D\uDCDC Here are the tasks in your list:\n");
        for (int i = 0; i < index; i++) {
            li.append("         ").append(i + 1).append(": ").append(list.get(i)).append("\n");
        }
        typeSetting(li.toString());
        return li.toString();
    }

    //The specific way to split string (exclusive for deadline and event")
    public static String[] stringSplitting(StringTokenizer st) {
        String temp = st.nextToken("/").substring(1);
        String description = temp.substring(0, temp.length() - 1);
        String byOrAt = st.nextToken("/").substring(3);
        return new String[]{description, byOrAt};
    }

    //to check whether the number input is valid
    public static boolean inRange(int num, int index) {
        return num > 0 && num <= index;
    }

    //To check whether the input for description is empty or not
    public static void checkDescription(String str, int i) throws EmptyDescriptionException {
        if (str.length() <= i + 1) {
            throw new EmptyDescriptionException("OOPS!!! The description of a task cannot be empty.");
        }
    }

    //get the corresponding Message indicated from the user input String
    public static Message getMessage(String str) throws InvalidKeyException {
        return Optional.ofNullable(availableMessage.get(str))
                .orElseThrow(() -> new InvalidKeyException("OOPS!!! I'm sorry, but I don't know what that means :-("));
    }

    //to process different requests which is decided by the first token of the message user entered
    public static List<Task> processRequest(String str, List<Task> list)
            throws InvalidKeyException, IllegalArgumentException, EmptyDescriptionException {
        if (str.equals("")) {
            throw new InvalidKeyException("Try to say something to me.");
        }

        int index = list.size();
        StringTokenizer st = new StringTokenizer(str);
        String first = st.nextToken(" ");

        switch (getMessage(first)) {
            //decide which action to be done by the first token
            case DONE:
                checkDescription(str, "done".length());
                int num = Integer.parseInt(str.substring(5));
                if (!inRange(num, index)) {
                    throw new IllegalArgumentException("OOPS!!! The number you checked for may not be valid.");
                }
                list.get(num - 1).markAsDone();
                typeSetting("    \uD83D\uDC4D Nice! I've marked this task as done: " + num
                        + "\n" + "      " + list.get(num - 1));
                break;

            case DELETE:
                checkDescription(str, "delete".length());
                int num2 = Integer.parseInt(str.substring(7));
                if (!inRange(num2, index)) {
                    throw new IllegalArgumentException("OOPS!!! The number you checked for may not be valid.");
                }
                Task t = list.remove(num2 - 1);
                index--;
                typeSetting("    \uD83D\uDC4C Noted. I've removed this task: " + num2
                        + "\n" + "      " + t + "\n" +
                        "    Now you have " + index + " tasks in the list.");
                break;

            case TODO:
                checkDescription(str, "todo".length());
                Todo td = new Todo(st.nextToken("").substring(1));
                list.add(td);
                index++;
                typeSetting("    \uD83D\uDFE2 Got it. I've added this task: \n      " +
                        td + "\n" +
                        "    Now you have " + index + " tasks in the list.");
                break;

            case DEADLINE:
                checkDescription(str, "deadline".length());
                String[] strings = stringSplitting(st);
                Deadline ddl = new Deadline(strings[0], strings[1]);
                list.add(ddl);
                index++;
                typeSetting("    \uD83D\uDD34 Got it. I've added this task: \n      " +
                        ddl + "\n" +
                        "    Now you have " + index + " tasks in the list.");
                break;

            case EVENT:
                checkDescription(str, "event".length());
                String[] strings2 = stringSplitting(st);
                Event ev = new Event(strings2[0], strings2[1]);
                list.add(ev);
                index++;
                typeSetting("    \uD83D\uDD35 Got it. I've added this task: \n      " +
                        ev + "\n" +
                        "    Now you have " + index + " tasks in the list.");
                break;

            default:
                throw new InvalidKeyException("OOPS!!! I'm sorry, but I don't know what that means :-(");
        }
        return list;
    }

    //to update the data file when any changes are made to the list of tasks
    public static void rewriteFile(String filePath, List<Task> list) throws IOException {
        FileWriter fw = new FileWriter(filePath);
        StringBuilder text = new StringBuilder();
        for (Task t: list) {
            List<String> details = new ArrayList<>() {{
                add(t.getClass().getSimpleName());
                add(t.getStatus());
                add(getSpecificDescription(t));
            }};
            text.append(String.join("~", details)).append("\n");
        }
        fw.write(text.toString());
        fw.close();
    }

    //get an extra piece of information if the Task is a Deadline or Event
    public static String getSpecificDescription(Task t) {
        String text = t.getDescription();
        if (t instanceof Deadline) {
            text += "~" + ((Deadline) t).getBy();
        } else if (t instanceof Event) {
            text += "~" + ((Event) t).getAt();
        }
        return text;
    }

    //get a Task according to the text in the data file
    public static Task decode(String data) {
        StringTokenizer st = new StringTokenizer(data);
        String className = st.nextToken("~");
        String status = st.nextToken("~");
        String description = st.nextToken("~");
        if (st.hasMoreTokens()) {
            String extra = st.nextToken("~");
            if (className.equals("Deadline")) {
                Deadline ddl =  new Deadline(description, extra);
                if (status.equals("1")) {
                    ddl.markAsDone();
                }
                return ddl;
            } else {
                Event ev =  new Event(description, extra);
                if (status.equals("1")) {
                    ev.markAsDone();
                }
                return ev;
            }
        }
        Todo td = new Todo(description);
        if (status.equals("1")) {
            td.markAsDone();
        }
        return td;
    }

    //load the list of tasks stored in hard disk
    public static List<Task> start(String filePath) throws IOException {
        List<Task> tasks = new ArrayList<>();
        List<String> data = Files.readAllLines(Paths.get(filePath));
        for (String s: data) {
            tasks.add(decode(s));
        }
        return tasks;
    }

    public static void main(String[] args) {
        //setting up
        Scanner sc = new Scanner(System.in);
        boolean exiting = false;
        List<Task> list = new ArrayList<>();
        try {
            list = start(taskData);
        } catch (IOException e) {
            System.err.println(e);
        }

        //welcome message and showing the list to the user
        typeSetting("    Hello, I'm Bob. \uD83D\uDC76 \uD83D\uDC76 \uD83D\uDC76\n    " +
                "What can I do for you? \uD83D\uDE03\n");
        gettingList(list);

        //talking to Bob
        String str = sc.nextLine();
        while (!exiting) {
            //check if the user want to exit
            while (!str.equals("bye")) {
                if (str.equals("list")) {
                    //print out the whole list
                    gettingList(list);
                } else {
                    //update the list of tasks
                    try {
                        list = processRequest(str, list);
                        rewriteFile(taskData, list);
                    } catch (InvalidKeyException | IllegalArgumentException |  EmptyDescriptionException
                            | IOException e) {
                        System.err.println(e);
                    }
                }
                str = sc.nextLine();
            }

            //exit confirmation
            typeSetting("    Are you sure you want to leave me alone? \uD83E\uDD7A (y/n)\n");

            if (sc.nextLine().equals("y")) {
                //confirm to leave and leaving message
                exiting = true;
                typeSetting("    Bye. Hope to see you again soon! \uD83D\uDE1E\n");
            } else {
                //not leaving and continue to interact with Bob
                typeSetting("    I know you are the best! \uD83D\uDE06\n    Then, what's next?\n");
                str = sc.nextLine();
            }
        }
    }
}
