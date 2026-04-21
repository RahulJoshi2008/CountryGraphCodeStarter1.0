import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

public class CountryGraph {
    // Adjacency List
    private Map<Country, Set<Country>> adjList;
    // Fast string-to-object lookup
    private Map<String, Country> countryLookup;

    public CountryGraph() {
        this.adjList = new HashMap<>();
        this.countryLookup = new HashMap<>();
    }

    // Helper: Add a single country to the graph safely
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

    // Shares an edge between two countries
    public void shareBorder(Country a, Country b) {
        addCountry(a);
        addCountry(b);
        adjList.get(a).add(b);
        adjList.get(b).add(a);
    }

    // Returns a Set of all unique edges
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

    // Uses BFS to find the shortest path
    public List<Country> findPath(Country start, Country end) {
        if (!adjList.containsKey(start) || !adjList.containsKey(end)) return null;
        if (start.equals(end)) return Collections.singletonList(start);

        Queue<Country> queue = new LinkedList<>();
        Map<Country, Country> parentMap = new HashMap<>(); // Keeps track of how we got to a node
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
        return null; // Path not found
    }

    private List<Country> reconstructPath(Map<Country, Country> parentMap, Country start, Country end) {
        List<Country> path = new ArrayList<>();
        Country current = end;
        while (current != null) {
            path.add(current);
            if (current.equals(start)) break;
            current = parentMap.get(current);
        }
        Collections.reverse(path); // Reverse so it goes Start -> End
        return path;
    }

    public boolean isConnected(Country a, Country b) {
        return findPath(a, b) != null;
    }

    public int getDistance(Country a, Country b) {
        List<Country> path = findPath(a, b);
        if (path == null) return -1; // Unreachable
        return path.size() - 1; // Distance is edges, not nodes
    }

    // BFS bounded by depth (radius)
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

    // Setup for CSV parsing
   public void loadData(String filename) {
        try {
            File file = new File(filename);
            // THIS IS THE MAGIC DEBUG LINE:
            System.out.println("Java is looking for the CSV file exactly here: " + file.getAbsolutePath()); 

            Scanner scanner = new Scanner(file);
            int count = 0;
            
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                String[] parts = line.split(","); 
                
                if (parts.length > 0) {
                    String mainCountryName = parts[0].trim();
                    Country mainCountry = new Country(mainCountryName, 0, "Unknown");
                    
                    for (int i = 1; i < parts.length; i++) {
                        String neighborName = parts[i].trim();
                        if (!neighborName.isEmpty()) {
                            Country neighbor = new Country(neighborName, 0, "Unknown");
                            shareBorder(mainCountry, neighbor);
                        }
                    }
                    count++;
                }
            }
            scanner.close();
            System.out.println("Successfully parsed " + count + " lines from the CSV.");
            
        } catch (FileNotFoundException e) {
            System.out.println("CRITICAL ERROR: " + filename + " was not found at the location above!");
        }
    }
}