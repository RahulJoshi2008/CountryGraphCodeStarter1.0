import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

public class CountryGraph {
    
    private Map<Country, Set<Country>> adjList;
    
    private Map<String, Country> countryLookup;

    public CountryGraph() {
        this.adjList = new HashMap<>();
        this.countryLookup = new HashMap<>();
    }

    
    private void addCountry(Country c) {
        if (!countryLookup.containsKey(c.getName())) {
            countryLookup.put(c.getName(), c);
            adjList.put(c, new HashSet<>());
        }
    }

    public Country getCountryByName(String name) {
        return countryLookup.get(name);
    }

    public Set<Country> getCountrySet() {
        return adjList.keySet();
    }

    
    public void shareBorder(Country a, Country b) {
        addCountry(a);
        addCountry(b);
        adjList.get(a).add(b);
        adjList.get(b).add(a);
    }

    
    public Set<Border> getBorderSet() {
        Set<Border> allBorders = new HashSet<>();
        for (Country c1 : adjList.keySet()) {
            for (Country c2 : adjList.get(c1)) {
                allBorders.add(new Border(c1, c2));
            }
        }
        return allBorders;
    }

    public Set<Country> getNeighbors(Country a) {
        return adjList.getOrDefault(a, new HashSet<>());
    }

    
    public List<Country> findPath(Country start, Country end) {
        if (!adjList.containsKey(start) || !adjList.containsKey(end)) return null;
        if (start.equals(end)) return Collections.singletonList(start);

        Queue<Country> queue = new LinkedList<>();
        Map<Country, Country> parentMap = new HashMap<>();
        Set<Country> visited = new HashSet<>();

        queue.add(start);
        visited.add(start);

        while (!queue.isEmpty()) {
            Country current = queue.poll();

            if (current.equals(end)) {
                return reconstructPath(parentMap, start, end);
            }

            for (Country neighbor : getNeighbors(current)) {
                if (!visited.contains(neighbor)) {
                    visited.add(neighbor);
                    parentMap.put(neighbor, current);
                    queue.add(neighbor);
                }
            }
        }
        return null; 
    }

    private List<Country> reconstructPath(Map<Country, Country> parentMap, Country start, Country end) {
        List<Country> path = new ArrayList<>();
        Country current = end;
        while (current != null) {
            path.add(current);
            if (current.equals(start)) break;
            current = parentMap.get(current);
        }
        Collections.reverse(path); 
        return path;
    }

    public boolean isConnected(Country a, Country b) {
        return findPath(a, b) != null;
    }

    public int getDistance(Country a, Country b) {
        List<Country> path = findPath(a, b);
        if (path == null) return -1; 
        return path.size() -1; 
    }


    public Set<Country> getWithinRadius(Country start, int radius) {
        Set<Country> result = new HashSet<>();
        if (!adjList.containsKey(start) || radius < 0) return result;

        Queue<Country> queue = new LinkedList<>();
        Map<Country, Integer> distanceMap = new HashMap<>();

        queue.add(start);
        distanceMap.put(start, 0);
        result.add(start);

        while (!queue.isEmpty()) {
            Country current = queue.poll();
            int currentDist = distanceMap.get(current);

            if (currentDist < radius) {
                for (Country neighbor : getNeighbors(current)) {
                    if (!distanceMap.containsKey(neighbor)) {
                        distanceMap.put(neighbor, currentDist + 1);
                        result.add(neighbor);
                        queue.add(neighbor);
                    }
                }
            }
        }
        return result;
    }

    
   public void loadData(String filename) {
        try {
            File file = new File(filename);
            
            System.out.println("looking for file @ : " + file.getAbsolutePath()); 

            Scanner scanner = new Scanner(file);
            int count = 0;
            
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                String[] parts = line.split(","); 
                
                if (parts.length > 0) {
                    String mainCountryName = parts[0].trim();
                    Country mainCountry = new Country(mainCountryName, 0, "null");
                    
                    for (int i = 1; i < parts.length; i++) {
                        String neighborName = parts[i].trim();
                        if (!neighborName.isEmpty()) {
                            Country neighbor = new Country(neighborName, 0, "null");
                            shareBorder(mainCountry, neighbor);
                        }
                    }
                    count++;
                }
            }
            scanner.close();
            System.out.println("parsed " + count + " lines from the csv");
            
        } catch (FileNotFoundException e) {
            System.out.println("" + filename + " was not found at the location above");
        }
    }
}