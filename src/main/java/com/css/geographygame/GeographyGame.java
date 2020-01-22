package com.css.geographygame;

import java.io.*;
import java.util.*;

/**
 * <h1>Geography Game</h1>
 * It's the end of the summer and school is just around the corner.
 * This is the time when we (and many other families) decide to take
 * a farewell-to-summer road trip. Hours in the car and children mix
 * like functional programming and mutable state or productivity and
 * open floor plans, so we need to find ways of keeping them
 * entertained that don't involve pushing each other out of a moving
 * vehicle, dead arms or spitting.
 * <p>
 * One of the games we like to play is called "Geography." The rules
 * are simple: one person names a city and the next person has to
 * name another city that begins with the last letter of his/her
 * opponent's city. So if I say "Newark", you can say "Kansas City"
 * and then I can say "Youngstown" and you can say "Noxville" and
 * then you lose because it's "Knoxvsille" you ignoramus! Eventually,
 * someone gets stuck and loses the game, which sometimes results in
 * crying and fighting.
 * </p>
 * <p>
 * I hate losing games, so I want to write a program to give me the
 * best chance of winning. Assume you have a dataset of all major
 * world cities like this one.
 * <a href="https://datahub.io/core/world-cities>Major Cities</a>
 * </p>
 * <p>
 * Your goal is to implement the function
 * <b>def nextMove(opponentsCity: String) : String</b>
 * which, given any city, will respond with your next move. Note
 * that your goal is not simply to play the game but to maximize your
 * chances of winning it. Also note that you will lose if you repeat
 * a city or use a city for which there is no valid response.
 * </p>
 *
 * <b>Solution:</b>
 * The program loads the list of cities in a Trie structure partitioned
 * by the last letter of the city name.  Partitioning by last letter
 * allows us to select a city name with the fewest responses.  In addition,
 * we need to keep track of the count of cities that start with a letter and
 * sort that list to efficiently look up the next city with the fewest
 * responses.
 *
 * @author  Craig Schwegel
 * @version 1.0
 * @since   2019-09-18
 */
public class GeographyGame {

    class CityNode
    {
        String name;
        String country;
        String subcountry;
        int geonameid;
        public CityNode(String _name, String _country, String _subcountry, int _geonameid)
        {
            name = _name;
            country = _country;
            subcountry = _subcountry;
            geonameid = _geonameid;
        }
        public int hashCode()
        {
            return geonameid;
        }
        public boolean equals(CityNode cn)
        {
            return (this.geonameid == cn.geonameid);
        }
    }
    class TrieNode
    {
        String character;
        boolean isCity;
        HashMap<String, TrieNode> childrenMap;
        List<CityNode> cities;
        public TrieNode(String _char)
        {
            character = _char;
            isCity = false;
            childrenMap = new HashMap<>();
            cities = new LinkedList<>();
        }
    }
    class NodeHead implements Comparable<NodeHead>
    {
        String key;
        int nbrOfCities;
        HashMap<String, TrieNode> childrenMap;
        TrieNode head;
        public NodeHead(String _key)
        {
            key = _key;
            nbrOfCities = 0;
            head = new TrieNode(_key);
            childrenMap = new HashMap<>();
        }

        @Override
        public int compareTo(NodeHead compareTo) {
            //Ascending order will give us the least number of responses available
            return this.nbrOfCities - compareTo.nbrOfCities;
        }
    }
    class Letter implements Comparable<Letter>
    {
        String key;
        int nbrOfCities;
        public Letter(String _key)
        {
            key = _key;
        }
        @Override
        public int compareTo(Letter o) {
            //Ascending order will give us the least number of responses available
            return this.nbrOfCities - o.nbrOfCities;
        }
        public int hashCode()
        {
            return key.hashCode();
        }
        public boolean equals(Letter letter)
        {
            return (this.key == letter.key);
        }
    }
    public static Properties props;
    public static String configFile;
    public static HashMap<String, NodeHead> headNodeMap = new HashMap<>();
    public static ArrayList<NodeHead> headNodeList = new ArrayList<>();
    public static ArrayList<Letter> firstLetterList = new ArrayList<>();
    public static HashMap<String, Letter> firstLetterMap = new HashMap<>();
    private static final boolean DebugFlag = true;
    public static void main(String[] args)
    {
        try {
            if (args == null || args.length == 0) {
                System.out.println("GeographyGame.main() expects a properties file as input.");
            }
            else
                configFile = args[0];

            //load properties file
            try (InputStream input = new FileInputStream(configFile)) {
                props = new Properties();
                // load a properties file
                props.load(input);
                props.forEach((key, value) -> System.out.println("Key : " + key + ", Value : " + value));
            } catch (IOException ex) {
                ex.printStackTrace();
            }

            init();
            GeographyGame gg = new GeographyGame();
            gg.loadData();
            gg.play();
        } catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public void playInternal()
    {
        try {
            String nextCity = "mazem";
            System.out.println("First Move::" + nextCity);
            while (true) {
                if (nextCity.startsWith("Loser") || nextCity.startsWith("Winner"))
                    break;
                nextCity = nextMove(nextCity);
                System.out.println("Move::" + nextCity);
            }
            System.out.println(nextCity);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static CharSequence DOUBLE_QUOTE = "\"";

    /**
     * parseLine() is implemented to handle values that are surrounded by
     * double quote in the CSV file so that commas that are not considered a
     * field delimiter are properly handled.  Ideally, there are many more cases
     * to handle here when sourcing a CSV in an ETL pattern.  There are many third
     * party libraries that do a great job dealing with the different characters
     * and cases.  However, since this is a coding challenge, I opted to handle the
     * one case I encountered during testing so that the class can be run with Java
     * and not require any third party libraries.
     * @param line
     * @return String[]
     */
    public static String[] parseLine(String line)
    {
        if (line.contains(DOUBLE_QUOTE))
        {
            try {
                StringBuilder sbField = new StringBuilder();
                String[] rtnArr = new String[4];
                int arrIndex = 0;
                boolean inDoubleQuote = false;
                char[] charLine = line.toCharArray();
                for (char ch : charLine) {
                    if (!inDoubleQuote && ch == ',') {
                        rtnArr[arrIndex++] = sbField.toString();
                        sbField = new StringBuilder();
                    } else if (!inDoubleQuote && ch == '"')
                        inDoubleQuote = true;
                    else if (inDoubleQuote && ch == '"')
                        inDoubleQuote = false;
                    else if (inDoubleQuote && ch != '"')
                        sbField.append(ch);
                    else
                        sbField.append(ch);
                }
                rtnArr[3] = sbField.toString();
                return rtnArr;
            } catch (Exception e)
            {
                System.out.println("parseLine()::Error processing line "+line);
            }
        }

        return line.split(",");
    }

    public void loadData()
    {
        try {
            BufferedReader bf = new BufferedReader(new FileReader(new File(fileName)));
            String sline = bf.readLine();
            if (DebugFlag)
                System.out.println("DEBUG::loadData()::Header = "+sline);
            sline = bf.readLine();
            while (sline != null && !sline.isEmpty())
            {
                //remove case sensitivity which can cause creation of more objects than necessary
                String[] dataArr = parseLine(sline.toLowerCase());

                CityNode cn = new CityNode(dataArr[0],dataArr[1],dataArr[2],Integer.parseInt(dataArr[3]));
                if (DebugFlag)
                    System.out.println("DEBUG::loadData()::Processing city name "+cn.name);
                if (cn.name != null && !cn.name.isEmpty()) {
                    //Partition trie structures by last letter in city name
                    String key = cn.name.substring(cn.name.length() - 1);
                    NodeHead nh = headNodeMap.get(key);
                    if (nh == null) {
                        nh = new NodeHead(key);
                        headNodeList.add(nh);
                        headNodeMap.put(key,nh);
                    }
                    TrieNode tn = nh.childrenMap.get(cn.name.substring(0,1));
                    if (tn == null) {
                        tn = new TrieNode(cn.name.substring(0, 1));
                        nh.childrenMap.put(cn.name.substring(0, 1), tn);
                    }
                    if (cn.name.length() == 1)
                    {
                        tn.cities.add(cn);
                        tn.isCity = true;
                    }
                    else
                        insertNode(tn,cn,cn.name.substring(1));
                    nh.nbrOfCities++;

                    //Store count based on first letter of city name
                    Letter letter = firstLetterMap.get(cn.name.substring(0,1));
                    if (letter == null) {
                        letter = new Letter(cn.name.substring(0, 1));
                        firstLetterList.add(letter);
                    }
                    letter.nbrOfCities++;
                    firstLetterMap.put(cn.name.substring(0,1),letter);
                }
                sline = bf.readLine();
            } //end while
        } catch (Exception ex)
        {
            ex.printStackTrace();
        }

        //sort ascending for least available responses
        Collections.sort(headNodeList);
        Collections.sort(firstLetterList);
    }

    public void insertNode(TrieNode curNode, CityNode cn, String parseName)
    {
        if (DebugFlag)
            System.out.println("DEBUG::insertNode()::Processing city name "+parseName);
        if (parseName == null || parseName.isEmpty())
            return;

        String key = parseName.substring(0,1);
        TrieNode tn = curNode.childrenMap.get(key);
        if (tn == null) {
            tn = new TrieNode(key);
            curNode.childrenMap.put(key, tn);
        }
        if (parseName.length() > 1)
            insertNode(tn,cn,parseName.substring(1));

        if (parseName.length() == 1) {
            tn.isCity = true;
            tn.cities.add(cn);
        }
        return;
    }

    public String nextMove(String opponentsCity)
    {
        opponentsCity = opponentsCity.toLowerCase();

        //No response check.  Opponent has run out of cities
        if (opponentsCity == null || opponentsCity.isEmpty())
            return "Winner. Opponent has run out of cities. Better luck next time.";

        System.out.println("opponentsCity = "+opponentsCity);

        //check if opponents city is a valid city and hasn't been used
        int listIndexNH = lookupCity(opponentsCity);
        if (listIndexNH == -1)
            return "Winner. Opponents city is not valid or was previously used.";
        NodeHead opponentsNh = headNodeList.get(listIndexNH);
        if (opponentsNh == null)
            return "Winner. Opponents city is not valid or was previously used.";

        //remove opponents city from Trie so it isn't used again
        //This is the case where the city name begins and ends with same letter i.e. onterio
        removeCity(opponentsCity);

        //Look up a valid response by finding a city that starts with the last letter
        //of the opponents city.  We look up starting with cities where the last letter
        //have the fewest responses to give us the best chance that the next opponent
        //won't be able to guess a response.
        String lastLetter = opponentsCity.substring(opponentsCity.length() - 1);
        NodeHead nh = null;
        TrieNode tn = null;

        for (int flIndex=0; flIndex<firstLetterList.size(); flIndex++)
        {
            //firstLetterList is sorted in ascending order so the fewest possible responses
            Letter letter = firstLetterList.get(flIndex);
            //find a NodeHead keyed by the last letter in each city ordered by ascending first letter cities
            nh = headNodeMap.get(letter.key);
            if (nh == null || nh.childrenMap.size() == 0)
                continue;
            tn = nh.childrenMap.get(lastLetter);
            if (tn == null)
                continue;

            //looks up if a city that starts with this letter exists
            CityNode cn = getCityNameFromTrie(tn);
            if (cn == null)
                continue;

            //need to check if a valid response exists to my response
            //you can't chose a city that ends with a last letter for which
            //there are no cities that start with the letter
            //otherwise, remove that city and pick another one continuing in least valid responses order
            if (hasValidResponseNotEqualCity(cn)) {
                //remove my city response from the dataset
                removeCity(cn.name);
                return cn.name;
            } else
                removeCity(cn.name);
        } //end for loop

        return "Loser. No valid response found that would give my opponent a valid choice.";
    }

    public static boolean hasValidResponseNotEqualCity(CityNode cn)
    {
        String lastLetter = cn.name.substring(cn.name.length()-1);
        for (NodeHead nh : headNodeList)
        {
            if (nh.nbrOfCities > 0 && nh.childrenMap.get(lastLetter) != null) {
                TrieNode tn = nh.childrenMap.get(lastLetter);
                CityNode nextCn = getCityNameFromTrieNotEqualCity(tn, cn);
                if (nextCn != null)
                    return true;
            }
        }
        return false;
    }

    public static void removeCity(String city)
    {
        int listIndexNH = lookupCity(city);
        NodeHead opponentsNh = headNodeList.get(listIndexNH);

        //remove opponents city from Trie so it won't be chosen again
        TrieNode opponentsTn = opponentsNh.childrenMap.get(city.substring(0,1));
        removeCityFromTrie(opponentsTn, city.substring(1));
        opponentsNh.nbrOfCities--;
        //clean up NodeHead if no entries
        if (opponentsNh.nbrOfCities == 0) {
            headNodeMap.remove(opponentsNh.key);
            headNodeList.remove(listIndexNH);
        }
        else
        {
            //re-sort list of nodes based on number of cities
            Collections.sort(headNodeList);
        }
        //decrement count by first letter and remove if count is 0
        Letter letter = firstLetterMap.get(city.substring(0,1));
        letter.nbrOfCities--;
        if (letter.nbrOfCities == 0)
        {
            firstLetterList.remove(letter);
            firstLetterMap.remove(letter.key);
        }
        else
            Collections.sort(firstLetterList);
    }

    public static int lookupCity(String city)
    {
        if (city == null || city.isEmpty())
            return -1;
        String firstLetter = city.substring(0,1).toLowerCase();
        String lastLetter = city.substring(city.length()-1);

        NodeHead nh = null;
        for (int i=0; i<headNodeList.size(); i++)
        {
            nh = headNodeList.get(i);
            if (!nh.key.equals(lastLetter))
                continue;
            else
            {
                if (nh.nbrOfCities > 0 && nh.childrenMap.containsKey(firstLetter)) {
                    if (findCityInTrie(nh.childrenMap.get(firstLetter), city.substring(1)))
                        return i;
                }
            }
        }

        return -1;
    }

    public static boolean findCityInTrie(TrieNode tn, String name)
    {
        if (tn == null)
            return false;

        if (name.length() > 0)
            return findCityInTrie(tn.childrenMap.get(name.substring(0,1)),name.substring(1));

        if (tn.isCity)
            return true;
        else
            return false;
    }

    public static void removeCityFromTrie(TrieNode tn, String city)
    {
        if (city.length() > 0)
            removeCityFromTrie(tn.childrenMap.get(city.substring(0,1)),city.substring(1));

        if (city.length() == 0)
        {
            tn.cities.remove(0);
            if (tn.cities.size() == 0)
                tn.isCity = false;
        }
        else {
            TrieNode oneDown = tn.childrenMap.get(city.substring(0, 1));
            if (oneDown != null &&
                    oneDown.isCity == false &&
                    oneDown.childrenMap.size() == 0)
                tn.childrenMap.remove(city.substring(0, 1));
        }
    }

    public static CityNode getCityNameFromTrie(TrieNode tn)
    {
        //if TrieNode contains a city then return first city in the list
        if (tn.isCity)
            return tn.cities.get(0);

        //TrieNode doesn't contain children. dead end.
        if (tn.childrenMap.isEmpty())
            return null;

        //Keep walking children and return the first city encountered
        for (TrieNode child : tn.childrenMap.values())
        {
            if (child != null)
                return getCityNameFromTrie(child);
        }
        return null;
    }

    public static CityNode getCityNameFromTrieNotEqualCity(TrieNode tn, CityNode notEqualCity)
    {
        //if TrieNode contains a city then return first city in the list
        //that doesn't equal this city
        if (tn.isCity) {
            CityNode cn = tn.cities.get(0);
            if (!cn.equals(notEqualCity))
                return cn;
            else
            {
                if (tn.cities.size() > 1)
                {
                    cn = tn.cities.get(1);
                    if (!cn.equals(notEqualCity))
                        return cn;
                }
            }
        }
        //TrieNode doesn't contain children. dead end.
        if (tn.childrenMap.isEmpty())
            return null;

        //Keep walking children and return the first city encountered
        for (TrieNode child : tn.childrenMap.values())
        {
            if (child != null)
                return getCityNameFromTrie(child);
        }
        return null;
    }

    /**
     * Controller inter-op code begin
     * Drop the code below into another Java program to play against another player
     * */
    public void play()
    {
        try {
            //Code is required to start the game
            //player 1 gets to name the first city
            String nextCity = startCity;
            if (filePrefixPlyr.equals("Player1")) {
                nextCity = nextCity.toLowerCase();
                writeNext(filePrefixPlyr, playerIndex, nextCity);
                removeCity(nextCity);
                playerIndex++;
            }
            while (true) {
                nextCity = readNext(filePrefixCTRL, playerIndex);
                nextCity = this.nextMove(nextCity);
                writeNext(filePrefixPlyr, playerIndex, nextCity);
                if (nextCity == null || nextCity.isEmpty() || nextCity.startsWith("Loser") || nextCity.startsWith("Winner"))
                    break;
                playerIndex++;
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public static void init() {
        startCity = props.getProperty("geo.player.startCity");
        fileName = props.getProperty("geo.player.datafile");
        playerIndex = Integer.parseInt(props.getProperty("geo.player.playerIndex"));
        filePrefixCTRL = props.getProperty("geo.player.filePrefixCTRL");
        filePrefixPlyr = props.getProperty("geo.player.filePrefixPlyr");
        gameDirectory  = props.getProperty("geo.player.gameDirectory");
        playDirectory = gameDirectory + System.getProperty("file.separator") + "play";
        readTimeout = Integer.parseInt(props.getProperty("geo.player.read.timeout"));
    }

    public static String startCity;
    public static String fileName;
    public static int playerIndex;
    public static String filePrefixCTRL;
    public static String filePrefixPlyr;
    public static String gameDirectory;
    public static String playDirectory;
    public static int readTimeout;

    public static void writeNext(String filePrefix, int index, String data) throws Exception
    {
        String nextFile = playDirectory + System.getProperty("file.separator") + filePrefix + "_" + index + ".txt";
        String nextTrigger = playDirectory + System.getProperty("file.separator") + filePrefix + "_TriggerRSP_" + index + ".txt";
        BufferedWriter bfWriter = new BufferedWriter(new FileWriter(new File(nextFile)));
        bfWriter.write(data);
        bfWriter.flush();
        bfWriter.close();
        BufferedWriter bfTriggerWriter = new BufferedWriter(new FileWriter(new File(nextTrigger)));
        bfTriggerWriter.write("TAG! You're it.");
        bfTriggerWriter.flush();
        bfTriggerWriter.close();
    }
    public static String readNext(String filePrefix, int index) throws Exception
    {
        int count = 0;
        File fPlayDir = new File(playDirectory);
        if (!fPlayDir.exists())
            fPlayDir.mkdirs();
        String responseTrigger = playDirectory + System.getProperty("file.separator") + filePrefix + "_Trigger_" + index + ".txt";
        File trigFile = new File(responseTrigger);
        //wait three seconds for a response
        while (count < readTimeout)
        {
            if (trigFile.exists())
                break;
            else
                Thread.sleep(100);
            count++;
        }
        String nextFile = playDirectory + System.getProperty("file.separator") + filePrefix + "_" + index + ".txt";
        BufferedReader bfReader = new BufferedReader(new FileReader(new File(nextFile)));
        String response = bfReader.readLine();
        bfReader.close();
        return response;
    }
    /**
    * End Controller inter-op code section
    * **/
}
