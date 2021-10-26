package MyApp.com;

public class AllRegisteredCarsBtnRow {
    String owner;
    String registered_car;

    public AllRegisteredCarsBtnRow(String registered_car, String owner) {
        this.owner = owner;
        this.registered_car = registered_car;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getRegistered_car() {
        return registered_car;
    }

    public void setRegistered_car(String registered_car) {
        this.registered_car = registered_car;
    }
}
