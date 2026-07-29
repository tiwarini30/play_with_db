import java.sql.*;

public class JDBCDemo {

    private static  final String URL="jdbc:mysql://localhost:3306/yourDB_name";
    private static  final String USER="root";
    private static  final String PASSWORD="YOURPASS";

    public static void main(String[] args) {
//        Connection conn = null;
        //normal try - catch block to connect with db
//        try{
//            conn = DriverManager.getConnection(URL,USER,PASSWORD);
//            System.out.println("Connected to the database");
//        }catch (SQLException e){
//            e.printStackTrace();
//        }finally {
//            try {
//                conn.close();
//                System.out.println("Connection close");
//            } catch (SQLException e) {
//                throw new RuntimeException(e);
//            }
//        }
        // conection with try with resource without close statment

        try(Connection conn = DriverManager.getConnection(URL,USER,PASSWORD);){
            System.out.println("Connected to the database");
            //insertStudent(conn, "alice", "a;lice@gmail.com");
            //insertStudent(conn, "tiwari", "tiwari@gmail.com");
            updateStudent(conn, 2,"bob","bob@gmail.com");

            selectStudent(conn);
           // deleteStudent(conn, 1);
        }catch (SQLException e) {
            e.printStackTrace();
        }
    }
    private static void insertStudent(Connection conn, String name, String email){
        String sql = "INSERT INTO students (name,  email) values('" +name+ "','" +email + "')";
        try(Statement stmt=conn.createStatement()){
            int rows=stmt.executeUpdate(sql);  //this is return int type
            System.out.println("Insert: "+rows);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    // select statment
    public static void selectStudent(Connection conn){
        String sql = "SELECT * FROM students";
        try(Statement stmt= conn.createStatement()){
            ResultSet resultSet = stmt.executeQuery(sql);
            System.out.println("Students list : ");
            while(resultSet.next()){
                int id=resultSet.getInt("id");
                String name=resultSet.getString("name");
                String email=resultSet.getString("email");
                System.out.println(id + " : "+ name + " : "+ email);
            }

        } catch (SQLException e) {
             e.printStackTrace();
        }
    }

    private static void updateStudent( Connection conn , int id , String name,String email){
        //String sql = " UPDATE students set name ='" + name + "', email = '"+email + "' where id =" +id ;
        // update students set name = 'alice', email='email.com'
        String sql = " UPDATE students set name = ?, email =  ? where id =  ?" ;
        try(PreparedStatement pstmt =conn.prepareStatement(sql)){
            pstmt.setString(1,name);
            pstmt.setString(2,email);
            pstmt.setInt(3,id);
            int row = pstmt.executeUpdate();
            System.out.println("upated : "+ row);
        } catch (SQLException e) {
             e.printStackTrace();
        }
    }
    private static void deleteStudent(Connection conn , int id ){
        String sql = " Delete from students where id ="+id;
        try(Statement stmt =conn.createStatement()){
            int row = stmt.executeUpdate(sql);
            System.out.println("Deleted : "+ row);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


}
