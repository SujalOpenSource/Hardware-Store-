package com.hardwarestore.billing;

import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class AmountInWordsService {
    private static final String[] SMALL = {"", "One", "Two", "Three", "Four", "Five", "Six",
            "Seven", "Eight", "Nine", "Ten", "Eleven", "Twelve", "Thirteen", "Fourteen",
            "Fifteen", "Sixteen", "Seventeen", "Eighteen", "Nineteen"};
    private static final String[] TENS = {"", "", "Twenty", "Thirty", "Forty", "Fifty",
            "Sixty", "Seventy", "Eighty", "Ninety"};

    public String toWords(BigDecimal amount) {
        if (amount == null) amount = BigDecimal.ZERO;
        long paisaTotal = amount.abs().setScale(2, RoundingMode.HALF_UP)
                .movePointRight(2).longValueExact();
        long rupees = paisaTotal / 100;
        int paisa = (int) (paisaTotal % 100);
        String result = (amount.signum() < 0 ? "Minus " : "") + integerToWords(rupees)
                + (rupees == 1 ? " Rupee" : " Rupees");
        if (paisa > 0) result += " and " + integerToWords(paisa)
                + (paisa == 1 ? " Paisa" : " Paisa");
        return result + " Only";
    }

    private String integerToWords(long number) {
        if (number == 0) return "Zero";
        if (number < 0) return "Minus " + integerToWords(-number);
        if (number >= 1_000_000_000L) return integerToWords(number / 1_000_000_000L)
                + " Billion " + integerToWords(number % 1_000_000_000L);
        if (number >= 1_000_000L) return integerToWords(number / 1_000_000L)
                + " Million " + integerToWords(number % 1_000_000L);
        if (number >= 1000) return integerToWords(number / 1000)
                + " Thousand " + integerToWords(number % 1000);
        if (number >= 100) return SMALL[(int) (number / 100)] + " Hundred "
                + integerToWords(number % 100);
        if (number >= 20) return TENS[(int) (number / 10)] + " " + SMALL[(int) (number % 10)];
        return SMALL[(int) number];
    }
}
