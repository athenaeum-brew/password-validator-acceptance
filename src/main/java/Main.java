class SuperType {
    public static void classMethod() {
        System.out.println(SuperType.class.getName());
    }

    public void instanceMethod() {
        System.out.println(this.getClass().getName());
    }

}

class SubType extends SuperType {

    public static void classMethod() {
        System.out.println(SubType.class.getName());
    }

    @Override
    public void instanceMethod() {
        System.out.println(this.getClass().getName());
    }
}

public class Main {
    public static void main(String[] args) {
        SuperType superType = new SuperType();
        superType.classMethod();
        superType.instanceMethod();

        SuperType inTheMiddle = new SubType();
        inTheMiddle.classMethod();
        inTheMiddle.instanceMethod();

        SubType subType = new SubType();
        subType.classMethod();
        subType.instanceMethod();
    }
}