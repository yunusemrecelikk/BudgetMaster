package de.deadlocker8.budgetmaster.databasemigrator.destination.category;


import de.deadlocker8.budgetmaster.databasemigrator.destination.TableNames;
import lombok.*;

import javax.persistence.*;

@Entity
@Table(name = TableNames.CATEGORY)
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
@ToString
public class DestinationCategory
{
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer ID;

	private String name;

	private String color;

	private Integer type;

	@Column(name = "icon_reference_id")
	private Integer iconReferenceID;
}
