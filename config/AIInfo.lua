--
--  Info Definition Table format
--
--
--  These keywords must be lowercase for LuaParser to read them.
--
--  key:      user defined or one of the SKIRMISH_AI_PROPERTY_* defines in
--            SSkirmishAILibrary.h
--  value:    the value of the property
--  desc:     the description (could be used as a tooltip)
--
--
--------------------------------------------------------------------------------
--------------------------------------------------------------------------------

local infos = {
	{
		key    = 'shortName',
		value  = 'FieldBOT',
		desc   = 'machine conform name.',
	},
	{
		key    = 'version',
		value  = 'dev', -- AI version - !This comment is used for parsing!
	},
	{
		key    = 'className',
		value  = 'fieldbot.FieldBOT',
		desc   = 'fully qualified name of a class that implements interface com.springrts.ai.AI',
	},
	{
		key    = 'name',
		value  = 'Peace field BOT',
		desc   = 'human readable name.',
	},
	{
		key    = 'description',
		value  = [[
Ecoing spam, no using army. Concentrating on base building.
Can undestand some human command (by chat and by markers). Share resources and units by demain.
Surface: recomended flat surface, can using ground unit or wather unit. Recomended non-mixed terrain on base.
Using: ground, wather units.
Using metal makers, can play on metal field map and non-metal map.
Average CPU usage: 0.7%-2.0%, some time 20.0% and lag 1 second.
Known support mod's: BA, TA, NOTA
Warning, Java AI Interface in spring 98 may be breaking and work unstable.
]],
		desc   = 'this should help to find out whether this AI is what they want',
	},
	{
		key    = 'loadSupported',
		value  = 'yes',
		desc   = 'whether this AI supports loading or not',
	},
	{
		key    = 'interfaceShortName',
		value  = 'Java', -- AI Interface name - !This comment is used for parsing!
		desc   = 'the shortName of the AI interface this AI needs',
	},
	{
		key    = 'interfaceVersion',
		value  = '0.1', -- AI Interface version - !This comment is used for parsing!
		desc   = 'the minimum version of the AI interface required by this AI',
	},
}

return infos
