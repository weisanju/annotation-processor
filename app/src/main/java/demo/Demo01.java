package demo;

import annotaionprocessor.NewIntent;

import java.util.List;

@NewIntent
public class Demo01 {
    private String name;
    private Integer age;

    private List<String> cc;


    public static void main(String[] args) {
        Demo01 demo01 = new Demo01();
        System.out.println(demo01);
    }
}
