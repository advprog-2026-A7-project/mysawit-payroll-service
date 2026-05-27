package com.mysawit.payroll.config;

import com.mysawit.payroll.model.WageConfig;
import com.mysawit.payroll.repository.WageConfigRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@ConditionalOnProperty(name = "payroll.seed.enabled", havingValue = "true")
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private WageConfigRepository wageConfigRepository;

    @Override
    public void run(String... args) throws Exception {

        // ── Seed WageConfig ───────────────────────────────────────────────────
        if (wageConfigRepository.count() == 0) {
            WageConfig buruh = new WageConfig("BURUH", 350.0, LocalDate.of(2026, 1, 1));
            buruh.setDescription("Upah panen per kg untuk buruh pemanen");
            buruh.setCreatedBy("admin");

            WageConfig supir = new WageConfig("SUPIR", 250.0, LocalDate.of(2026, 1, 1));
            supir.setDescription("Upah angkut per kg untuk supir truk");
            supir.setCreatedBy("admin");

            WageConfig mandor = new WageConfig("MANDOR", 150.0, LocalDate.of(2026, 1, 1));
            mandor.setDescription("Insentif per kg untuk mandor/pengawas");
            mandor.setCreatedBy("admin");

            wageConfigRepository.save(buruh);
            wageConfigRepository.save(supir);
            wageConfigRepository.save(mandor);

            System.out.println("✓ Seeded WageConfig: BURUH=350/kg, SUPIR=250/kg, MANDOR=150/kg");
        }
    }
}
