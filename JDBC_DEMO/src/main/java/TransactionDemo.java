import java.sql.*;

public class TransactionDemo {

    private static  final String URL="jdbc:mysql://localhost:3306/yourDB_name";
    private static  final String USER="root";
    private static  final String PASSWORD="YOUR_PAss";
// database connection with try resource
    public static void main(String[] args) {
        try(Connection conn = DriverManager.getConnection(URL,USER,PASSWORD)){
            System.out.println("Connected to the database");
            // transcation process begin---
            // turned off auto commit that means NO auto save
            conn.setAutoCommit(false);
            try{
                //two table add operation on this orders , orderItems
                // insert into orders
                int orderId= insertOrder(conn,101,"alice01",2000.0);
                //insert into order_items
                insertOrderItems(conn, orderId,"laptop",1,2000.0);
                // manuually commit
                conn.commit();
                System.out.println("transcation committed successfully --- ");
            } catch (Exception e) {
             e.printStackTrace();
             conn.rollback();
                System.out.println("operation roll back ");
        } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
             e.printStackTrace();
        }
    }

    private static int insertOrder(Connection conn, int customerId, String customerName, double price) {
        String sql = "INSERT INTO orders(user_id, customer_name,total_amount) VALUES (?,?,?)";
        try(PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)){  // auto_increament
            pstmt.setInt(1,customerId);
            pstmt.setString(2,customerName);
            pstmt.setDouble(3,price);
            int rows = pstmt.executeUpdate();
            System.out.println("INSERTED into orders: " + rows);
            // generate orderID
            try(ResultSet rs = pstmt.getGeneratedKeys()){
                if(rs.next()){
                    int orderId=rs.getInt(1);
                    System.out.println("ORDERED ID: "+ orderId);
                    return orderId;
                }else{
                    throw new SQLException("ORDER ID not generated... ! ");
                }
            }
        } catch (SQLException e) {
             throw new RuntimeException();
        }
    }

    private static void insertOrderItems(Connection conn, int orderId, String productName, int quantity, double price) {
        String sql = "INSERT INTO order_items(order_id, product_name,quantity, price) VALUES (?,?,?,?)";
        try(PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)){  // auto_increament
            pstmt.setInt(1,orderId);
            pstmt.setString(2,productName);
            pstmt.setInt(3,quantity);
            pstmt.setDouble(4,price);

            //int x=10/0; // if we throw exception
            int rows = pstmt.executeUpdate();
            System.out.println("INSERTED into order_items: " + rows);
            // generate orderID

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

}
