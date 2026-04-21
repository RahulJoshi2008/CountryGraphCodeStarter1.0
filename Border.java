import java.util.Objects;

public class Border {
    private Country countryA;
    private Country countryB;

    public Border(Country countryA, Country countryB) {
        this.countryA = countryA;
        this.countryB = countryB;
    }

    public Country getCountryA() { return countryA; }
    public Country getCountryB() { return countryB; }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Border border = (Border) obj;
        
        return (Objects.equals(countryA, border.countryA) && Objects.equals(countryB, border.countryB)) ||
               (Objects.equals(countryA, border.countryB) && Objects.equals(countryB, border.countryA));
    }

    @Override
    public int hashCode() {
        return countryA.hashCode() + countryB.hashCode(); 
    }

    @Override
    public String toString() {
        return countryA.getName() + " <-> " + countryB.getName();
    }
}