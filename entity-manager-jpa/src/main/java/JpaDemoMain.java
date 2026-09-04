import jakarta.persistence.EntityManager;

public class JpaDemoMain {
    public static void main(String[] args) {
        EntityManager em= JPAUtil.getEntityManager();
        try{
            UserClassJPA user = new UserClassJPA(1L,"alice");
            em.getTransaction().begin();
            em.persist(user);
            em.getTransaction().commit();
            System.out.println("User saved : "+user.getId());
        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            em.close();
            JPAUtil.close();
        }
    }
}
