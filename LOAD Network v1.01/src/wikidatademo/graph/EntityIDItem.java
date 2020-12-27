package wikidatademo.graph;

public class EntityIDItem {
    public int id;
    public String type;

    public EntityIDItem (int id, String type) {
        this.id = id;
        this.type = type;
    }

    @Override
    public boolean equals(Object o) {
        EntityIDItem i = (EntityIDItem) o;
        return (id == i.id && type.equals(i.type));
    }

    @Override
    public int hashCode() {
        return id; // max 7 collisions due to 7 sets of entities
    }
}
