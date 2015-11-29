MGU_parameter {

	classvar instanceCount;

	var <container, <name, <type, <range, <default, <>alwaysOnServ;
	var <>inUnit, <>outUnit, <>sr;
	var <>defaultNode;
	var <val;
	var <>address, <>defName;
	var oscFunc, <>oscPort;
	var <>parentAccess;
	var listening, netaddr_responder, responder_device;
	var <>bound_to_ui, <>ui, ui_type;
	var <>description;


	*new { |container, name, type, range, default, alwaysOnServ = false,
		inUnit, outUnit, sr = 44100|
		^this.newCopyArgs(container, name, type, range, default, alwaysOnServ,
			inUnit, outUnit, sr).init
	}

	init {

		instanceCount !? { instanceCount = instanceCount + 1 };
		instanceCount ?? { instanceCount = 1 };

		bound_to_ui = false;
		listening = false;
		description = "no description available";

		// register to parent container
		container.registerParameter(this);
		this.val_(default, onServ: false);

		// init OSC Function
		oscFunc = OSCFunc({|msg, time, addr, recvPort|
			msg.postln;
			this.val_(msg[1])}, address, nil, oscPort);

	}

	unitCheck { |value|

		switch(inUnit,

			\s, { switch(outUnit,
				\ms, { val = value * 1000 },
				\samps, { val = value * sr })},
			\ms, { switch(outUnit,
				\s, { val = value / 1000 },
				\samps, { val = (value / 1000) * sr})},
			\samps, { switch(outUnit,
				\s, { val = value / sr },
				\ms, { val = (value / sr) * 1000 })},
			\dB, {if(outUnit == \amp, { val = value.dbamp })},
			\amp, {if(outUnit == \dB, { val = value.ampdb })});

	}

	val_ { |value, node, interp = false, duration = 2000, curve = \lin, onServ,
		absolute_unit = false, report_to_ui = true|

		var process;
		process = {
			value.size.do({|i|
				// casts & type tests
				if((value[i] == inf) || (value[i] == -inf), {
					value.put(i, value[i].asInteger)});

				if((value[i].isKindOf(Integer)) && (type == Float), {
					value.put(i, value[i].asFloat)});

				if((value[i].isKindOf(Float)) && (type == Integer), {
					value.put(i, value[i].asInteger)});

				if((value[i].isKindOf(type) == false) && (type != Array), {
					Error("[PARAMETER] /!\ WRONG TYPE:" + name).throw });

				// range check
				if((value[i].isKindOf(Integer)) || (value[i].isKindOf(Float)), {
					value[i] = value[i].clip(range[0], range[1]);
				});

				if(type == Array, { val = value }, { val = value[i] });

				// unit conversion
				if((inUnit.notNil) && (outUnit.notNil) && (absolute_unit == false), {
					this.unitCheck(val)
				});

				// sending value on server
				if(onServ, {
					node[0] ?? { node = [defaultNode] };
					node[0] ?? { Error("[PARAMETER] /!\ NODE NOT DEFINED" + name).throw };
					node[i].set(defName, val);
				});
			});

			// call parent methods
			parentAccess !? { parentAccess.paramCallBack(name, value)};

			// reply to minuit listening
			if(listening,
				{ netaddr_responder.sendBundle(nil, [responder_device ++ ":listen",
					address ++ ":value", val].postln)});

			// reply to gui element
			if((bound_to_ui) && (report_to_ui)) { ui.value_from_parameter(val) };

		};

		// STARTS HERE
		onServ ?? { if(alwaysOnServ, { onServ = true }, { onServ = false })};

		// node & value always as array
		if(node.isArray == false, { node = [node] });

		// if value is a function
		if(value.isFunction, {
			var func, currentVal = [];
			func = value;
			if(onServ, {
				node.size.do({|i|
					node[i].get(defName.asSymbol, {|kval|
						currentVal = currentVal.add(kval);
						value = func.value(currentVal);
						process.value})}, { // else -> not on server
						value = func.value(val);
						process.value});
			});
			}, { if(value.isArray == false, { value = [value] });  // else : not a function
			 	 process.value});

	}

	enableListening { |netaddr, device| // accessed by minuit interfaces only
		listening = true;
		netaddr_responder ?? { netaddr_responder = netaddr };
		responder_device ?? { responder_device = device };
	}

	disableListening {
		listening = false;
	}


	// DEF MTHODS (change to kr, ar etc.)

	smb {
		^defName.asSymbol
	}

	kr {
		^defName.asSymbol.kr
	}

	ar {
		^defName.asSymbol.ar
	}

	tr {
		^defName.asSymbol.tr
	}

	defNameKr {
		^("\\" ++ defName ++ ".kr");
	}

	defNameAr {
		^("\\" ++ defName ++ ".ar");
	}

	defNameTr {
		^("\\" ++ defName ++ ".tr");
	}

}