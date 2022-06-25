package demo;

import annotaionprocessor.Data;

import java.util.List;

@Data
public class Demo01 {
    private String name;
    private Integer age;

    private List<String> cc;

    public Integer getAge() {
        System.out.println(1);
        return age;
    }

    @Override
    public String toString() {
        return "Demo01{" +
                "name='" + name + '\'' +
                ", age=" + age +
                ", cc=" + cc +
                '}';
    }

    public static void main(String[] args) {
        Demo01 demo01 = new Demo01();

        System.out.println(demo01);
    }
}
