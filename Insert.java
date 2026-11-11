import java.sql.*;
import java.util.*;
 public class Insert
{
	public static void main(String args[])
	{
		try{
			Scanner s=new Scanner(System.in);
		Class.forName("oracle.jdbc.OracleDriver");
		System.out.println("Driver Loaded successfully");
		Connection con=DriverManager.getConnection("jdbc:oracle:thin:@localhost:1521:xe","system","system");
		System.out.println("connected successfully");
		Statement stmt=con.createStatement();
		System.out.println("statement created successfully");
		String sql="insert into vijay values(?,?,?);
		System.out.println("enter no of records to insert");
		int n=s.nextInt();
		System.out.println("enter values to insert");
		PreparedStatement ps=con.PreapredStatement(sql);
		int i=1;
		while(i<=n)
		{
			int id=s.nextInt();
			String name=s.next();
			int salary=s.nextInt();
			ps.setInt(1,id);
			ps.setString(2,name);
			ps.setInt(3,salary);
			int r=ps.executeUpdate();
			if(r>0)
			System.out.println("row inserted successfully");
			i++;
		
		}
		if(i>n)
		System.out.println("all are inserted");
	}
		catch(Exception e)
		{
			System.out.println("Driver not loaded successfully"+e);
		}
	}
}