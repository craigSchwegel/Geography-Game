package com.css.geographygame;

import java.io.*;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Properties;
import java.util.Set;

public class GeoGameController {

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
        public boolean equals(GeographyGame.CityNode cn)
        {
            return (this.geonameid == cn.geonameid);
        }
    }
    public static Properties props;
    public static String fileName;
    public static String configFile;
    public static String gameDirectory;
    public static String playDirectory;
    public static int readTimeout;

    public static void main(String[] args)
    {
        try {
            if (args == null || args.length == 0) {
                System.out.println("GeographyGame.main() expects a properties files as input.");
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

            gameDirectory = props.getProperty("geo.controller.gameDirectory");
            fileName = props.getProperty("geo.controller.datafile");
            playDirectory = gameDirectory + System.getProperty("file.separator") + "play";
            readTimeout = Integer.parseInt(props.getProperty("geo.controller.read.timeout"));

            File playDir = new File(playDirectory);
            if (!playDir.exists())
                playDir.mkdir();
            GeoGameController GGOps = new GeoGameController();
            GGOps.loadData();
            GGOps.printNumberOfCitiesInMap();
            System.out.println("Size of CityMap = "+cityMap.size());
            GGOps.play();
        } catch (Exception e)
        {
            e.printStackTrace();
        }
    }
    private static CharSequence DOUBLE_QUOTE = "\"";

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
    public static HashMap<String, LinkedList<CityNode>> cityMap = new HashMap<>();
    public static HashMap<String, Integer> cityMapByFirstLetter = new HashMap<>();
    public void loadData()
    {
        try {
            BufferedReader bf = new BufferedReader(new FileReader(new File(fileName)));
            String sline = bf.readLine();
            sline = bf.readLine();
            while (sline != null && !sline.isEmpty())
            {
                //remove case sensitivity which can cause creation of more objects than necessary
                String[] dataArr = parseLine(sline.toLowerCase());

                CityNode cn = new CityNode(dataArr[0],dataArr[1],dataArr[2],Integer.parseInt(dataArr[3]));
                if (cn.name != null && !cn.name.isEmpty()) {
                    LinkedList<CityNode> ll = null;
                    if (cityMap.containsKey(cn.name)) {
                        ll = cityMap.get(cn.name);
                        ll.add(cn);
                    }
                    else
                    {
                        ll = new LinkedList<>();
                        ll.add(cn);
                        cityMap.put(cn.name,ll);
                    }
                    /**********/
                    String firstLetter = cn.name.substring(0,1);
                    Integer cityCount = cityMapByFirstLetter.get(firstLetter);
                    if (cityCount == null)
                        cityCount = new Integer(0);
                    cityCount = new Integer(cityCount.intValue() + 1);
                    cityMapByFirstLetter.put(firstLetter, cityCount);
                    /***** end *****/
                }
                sline = bf.readLine();
            } //end while
        } catch (Exception ex)
        {
            ex.printStackTrace();
        }
    } //end loadData()

    public void printNumberOfCitiesInMap()
    {
        int count = 0;
        Set<String> keys = cityMap.keySet();
        for (String key : keys)
        {
            LinkedList<CityNode> cities = cityMap.get(key);
            if (cities != null)
                count += cities.size();
        }
        System.out.println("printNumberOfCitiesInMap():: Nbr of cities = "+count);
    }
    public int playerOneIndex = 1;
    public int playerTwoIndex = 1;


    public static void writeNext(String filePrefix, int index, String data) throws Exception
    {
        String nextFile = playDirectory + System.getProperty("file.separator") + filePrefix + "_" + index + ".txt";
        String nextTrigger = playDirectory + System.getProperty("file.separator") + filePrefix + "_Trigger_" + index + ".txt";
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
        String responseTrigger = playDirectory + System.getProperty("file.separator") + filePrefix + "_TriggerRSP_" + index + ".txt";
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
    public void play()
    {
        try
        {
            String nextCity = "XXXXXXX";

            while (nextCity != null && !nextCity.isEmpty())
            {
                Thread.sleep(10);
                nextCity = readNext("Player1", playerOneIndex);
                nextCity = nextCity.toLowerCase();
                System.out.println("Player1 response is " + nextCity);
                //check if valid city
                if (!checkAndRemoveValidCity(nextCity))
                {
                    System.out.println("Player1 provided invalid response. GAME OVER!");
                    break;
                }
                if (!checkIfCityHasValidResponse(nextCity))
                {
                    System.out.println("Player2 provided city with no valid response. GAME OVER!");
                    break;
                }
                writeNext("CTRLPlayer2", playerTwoIndex, nextCity);

                Thread.sleep(10);

                nextCity = readNext("Player2", playerTwoIndex);
                System.out.println("Player2 response is " + nextCity);
                //check if valid city
                if (!checkAndRemoveValidCity(nextCity))
                {
                    System.out.println("Player2 provided invalid response. GAME OVER!");
                    break;
                }
                if (!checkIfCityHasValidResponse(nextCity))
                {
                    System.out.println("Player2 provided city with no valid response. GAME OVER!");
                    break;
                }
                playerOneIndex++;
                playerTwoIndex++;
                writeNext("CTRLPlayer1", playerOneIndex, nextCity);
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
    public boolean checkAndRemoveValidCity(String city)
    {
        LinkedList<CityNode> cities = cityMap.get(city);
        if (cities == null)
            return false;

        for (CityNode cn : cities)
        {
            if (cn.name.equals(city))
            {
                //remove city if it exists
                cities.remove(cn);
                return true;
            }
        }
        System.out.println("ERROR::Check why code is hitting this line!!!!!!!!!!!!!!!!");
        return false;
    }

    public boolean checkIfCityHasValidResponse(String city)
    {
        if (city == null || city.isEmpty())
            return false;
        String lastLetter = city.substring(city.length()-1);

        Integer cityCount = cityMapByFirstLetter.get(lastLetter);
        if (cityCount == null)
            return false;
        else if (cityCount.intValue() == 0) {
            cityMapByFirstLetter.remove(lastLetter);
            return false;
        }
        else {
            cityCount = new Integer(cityCount.intValue()-1);
            cityMapByFirstLetter.put(lastLetter,cityCount);
            return true;
        }
    }
}
