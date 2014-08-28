--
-- Custom Options Definition Table format
--
-- A detailed example of how this format works can be found
-- in the spring source under:
-- AI/Skirmish/NullAI/data/AIOptions.lua
--
--------------------------------------------------------------------------------
--------------------------------------------------------------------------------

local options = {
	{
		key  = 'commandbyenemy',
		name = 'Allow enemy command',
		desc = 'AI can control by enemy team chat command. If false - only team chat.',
		type = 'bool',
		def  = false
	},

	{
		key  = 'autotechup',
		name = 'Allow auto tech up',
		desc = 'Automatic do tech up to new level. If false - type "bot do tech up" for manual do it.',
		type = 'bool',
		def  = false
	},
	{
		key  = 'smarttechup',
		name = 'Smart tech up time',
		desc = 'Use smart algorithm for detect optimal tech up time. Required more CPU time.',
		type = 'bool',
		def  = false
	},

	{ -- list
		key     = 'builddecorator',
		name    = 'Build decorator type: how build base planing',
		desc    = 'How choose building position on base surface.\nSpiral, krugi, tetris, random as other AI',
		type    = 'list',
		section = 'performance',
		def     = 'random',
		items   = {
			{
				key  = 'random',
				name = 'Random',
				desc = 'random razpredelenie (tupo).',
			},
			{
				key  = 'spiral',
				name = 'Spiral',
				desc = 'The AI will build on spirale.',
			},
			{
				key  = 'circle',
				name = 'Circle',
				desc = 'The AI will build on circles around.',
			},
			{
				key  = 'tetris',
				name = 'Tetris',
				desc = 'The AI will be smart play on sim-city :)',
			}
		}
	},
	{
            key="messagelevel",
            name="Show message level",
            desc="Log level. 0 - don't speak, 1-dialog, 2-+error, 3,4-+debug info.",
            type   = 'number',
            def    = 2,
            min    = 0,
            max    = 4,
            step   = 1
         },
	 {
            key="metaldetect",
            name="Metal detect map",
            desc="-2 autodetect, -1 no metal map, 0 normal map, 1 metal field.",
            type   = 'number',
            def    = -2,
            min    = -2,
            max    = 1,
            step   = 1
         }
}

return options

