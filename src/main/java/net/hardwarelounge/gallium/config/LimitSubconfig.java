package net.hardwarelounge.gallium.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class LimitSubconfig {
    private String name;
    private Map<String, Integer> limit;
}
