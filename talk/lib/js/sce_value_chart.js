$(function () {

	

	
$('#c1').highcharts({
    title: {
        text: 'SCE Value Curves',
        x: -20 //center
    },
    subtitle: {
        text: 'Comparison of Service Offerings (beyond base subscription)',
        x: -20
    },
    xAxis: {
        categories: ['Cost', 'Availability', 'OperationalResource', 'DedicatedContact', 'CustomerKnowledge', 'TechAdvisor', 'CommunicationFreq','Onsites',
            'CaseMgmt', 'POCs', 'Advocacy', 'SpecialAccess', 'Certification', 'Roadmap&Lifecycle', 'ExpeditedPatching']
    },
    yAxis: {
        title: {
            text: 'Engagement Level'
        },
        plotLines: [{
            value: 0,
            width: 1,
            color: '#808080'
        }]
    },
    tooltip: {
        valueSuffix: 'Engagement'
    },
    legend: {
        layout: 'vertical',
        align: 'right',
        verticalAlign: 'middle',
        borderWidth: 0
    },
    series: [{
        name: 'TAM',
		//     cost 24x7 op  dedc  kno  Adv  call site cmgt poc, adv  spa  cert rmap patching
        data: [3.0, 2.0, 3.0, 3.0, 3.0, 3.0, 3.0, 2.0, 3.0, 2.0, 2.0, 2.0, 0.0, 2.0, 2.0]
    }, {
        name: 'SRM',
        data: [0.0, 2.0, 0.0, 3.0, 2.0, 0.0, 3.0, 1.0, 3.0, 1.0, 2.0, 0.0, 0.0, 2.0, 1.0]
    }, {
        name: 'Subscription',
        data: [2.0, 3.0, 3.0, 0.0, 1.0, 3.0, 1.0, 0.0, 0.0, 1.0, 1.0, 0.0, 0.0, 1.0, 1.0]
    }, {
        name: 'Partner/ISV SRM',
        data: [1.0, 2.0, 0.0, 3.0, 2.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 3.0, 2.0, 3.0, 1.0]
    }]
});
});