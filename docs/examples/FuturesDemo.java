import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class FuturesDemo {
    record OrderPage(List<String> products, List<String> customers) {}

    public static void main(String[] args) throws Exception {
        // Java 21: close this demo's executor when main finishes using it.
        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Future<List<String>> future = executor.submit(FuturesDemo::loadProducts);
            // Do other work here; get() waits if the result is not ready.
            System.out.println("Future products: " + future.get());

            // Submit both independent operations before waiting for either.
            CompletableFuture<List<String>> products =
                    CompletableFuture.supplyAsync(FuturesDemo::loadProducts, executor);
            CompletableFuture<List<String>> customers =
                    CompletableFuture.supplyAsync(FuturesDemo::loadCustomers, executor);

            CompletableFuture<OrderPage> page =
                    products.thenCombine(customers, OrderPage::new);

            // Wait only at this standalone program's final consumption point.
            System.out.println("Combined page: " + page.join());
        }
    }

    private static List<String> loadProducts() {
        return List.of("Keyboard", "Mouse");
    }

    private static List<String> loadCustomers() {
        return List.of("Alice", "Bob");
    }
}
