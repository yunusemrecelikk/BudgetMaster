package de.deadlocker8.budgetmaster.database.importer;

import de.deadlocker8.budgetmaster.icon.Icon;
import de.deadlocker8.budgetmaster.icon.IconRepository;
import de.deadlocker8.budgetmaster.services.EntityType;

public class IconImporter extends ItemImporter<Icon>
{
	public IconImporter(IconRepository iconRepository)
	{
		super(iconRepository, EntityType.ICON, true);
	}

	@Override
	protected int importSingleItem(Icon icon)
	{
		if(!(repository instanceof IconRepository repository))
		{
			throw new IllegalArgumentException("Invalid repository type");
		}

		final Icon iconToCreate = new Icon();
		iconToCreate.setImage(icon.getImage());
		iconToCreate.setBuiltinIdentifier(icon.getBuiltinIdentifier());

		final Icon savedIcon = repository.save(iconToCreate);

		return savedIcon.getID();
	}

	@Override
	protected String getNameForItem(Icon item)
	{
		return String.valueOf(item.getID());
	}
}
