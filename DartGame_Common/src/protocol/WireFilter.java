package protocol;
import java.io.ObjectInputFilter;

public final class WireFilter {
    private WireFilter() {}
    public static ObjectInputFilter create() {
        return info -> {
            if (info.depth() > 20 || info.arrayLength() > 10000) return ObjectInputFilter.Status.REJECTED;
            Class<?> c = info.serialClass();
            if (c == null) return ObjectInputFilter.Status.UNDECIDED;
            while (c.isArray()) c = c.getComponentType();
            String n = c.getName();
            boolean allowed = c.isPrimitive() || n.startsWith("protocol.") || n.startsWith("enums.")
                    || n.equals("java.lang.String") || n.equals("java.lang.Object")
                    || n.equals("java.lang.Enum") || n.equals("java.lang.Number")
                    || n.equals("java.lang.Long") || n.equals("java.lang.Integer")
                    || n.equals("java.lang.Boolean") || n.equals("java.util.ArrayList");
            return allowed ? ObjectInputFilter.Status.ALLOWED : ObjectInputFilter.Status.REJECTED;
        };
    }
}
