import java.util.Objects;

public class Country {
    private String name;
    private long population;
    private String continent;

    public Country(String name, long population, String continent) {
        this.name = name;
        this.population = population;
        this.continent = continent;
    }

    public String getName() { 
        return name; 
    }

    public long getPopulation() { 
        return population; 
    }

    public String getContinent() { 
        return continent; 
    }

    public void setName(String name) { 
        this.name = name; 
    }

    public void setPopulation(long population) { 
        this.population = population; 
    }

    public void setContinent(String continent) { 
        this.continent = continent; 
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;

        if (obj == null || getClass() != obj.getClass()) return false;

        Country country = (Country) obj;
        return Objects.equals(name, country.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    
    @Override
    public String toString() {
        return name;
    }
}