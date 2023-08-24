package de.ait.final_project.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateCakeDto {

    @NotBlank
    @Schema(example = "vanilla-cupcake")
    private String name;

    @NotBlank
    @Schema(example = "sugar...")
    private String ingredients;

    @NotBlank
    @Schema(example = "57.90")
    private Double price;

}
