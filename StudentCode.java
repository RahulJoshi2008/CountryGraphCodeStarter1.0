import java.util.*;

public class StudentCode extends Server {
    
    // Instantiate our custom backend engine
    private CountryGraph graph;

public StudentCode() {
        super(); 
        this.graph = new CountryGraph();
        graph.loadData("CountryBorders.csv"); 
        
        // ADD THIS LINE:
        System.out.println("Graph initialized. Total countries loaded: " + graph.getCountrySet().size());
    }

    public static void main(String[] args) {
        Server server = new StudentCode(); 
        server.run(); // Start the server
        server.openURL(); // Open url in browser
    }

    @Override
    public void getInputCountries(String country1, String country2) {
        // Clear previous interactions from the map
        clearCountryColors();
        
        // Fetch nodes from the graph
        Country start = graph.getCountryByName(country1);
        Country end = graph.getCountryByName(country2);
        
        if (start == null || end == null) {
            sendMessageToUser("Error: One or both countries not found in the dataset.");
            setMessage("Error finding path.");
            return;
        }

        // Run Breadth-First Search to find the shortest path
        List<Country> path = graph.findPath(start, end);
        
        if (path == null) {
            sendMessageToUser("No land path exists between " + country1 + " and " + country2 + ".");
            setMessage("No path found.");
        } else {
            int distance = path.size() - 1;
            sendMessageToUser("Path found! It takes " + distance + " borders to cross from " + country1 + " to " + country2 + ".");
            setMessage("Borders crossed: " + distance);
            
            // Color the path nodes for the UI
            for (int i = 0; i < path.size(); i++) {
                Country c = path.get(i);
                if (i == 0) {
                    addCountryColor(c.getName(), "green");     // Start node
                } else if (i == path.size() - 1) {
                    addCountryColor(c.getName(), "red");       // End node
                } else {
                    addCountryColor(c.getName(), "yellow");    // Intermediate path nodes
                }
            }
        }
    }

    @Override
    public void getColorPath() {
        // Required by the abstract Server class. 
        // Can be left empty unless you want to return specific Map data structures directly.
    }

    @Override
    public void handleClick(String countryName) {
        // Reset the map canvas
        clearCountryColors();
        
        // Fetch the clicked country from our graph
        Country country = graph.getCountryByName(countryName);
        
        if (country != null) {
            // Highlight the clicked country
            addCountryColor(countryName, "blue"); 
            
            // Highlight all immediate neighbors
            Set<Country> neighbors = graph.getNeighbors(country);
            for (Country neighbor : neighbors) {
                addCountryColor(neighbor.getName(), "lightblue");
            }
            
            sendMessageToUser(countryName + " selected. It shares a land border with " + neighbors.size() + " countries.");
            setMessage("Selected: " + countryName);
        } else {
            sendMessageToUser("Graph data not found for: " + countryName);
        }
    }
}