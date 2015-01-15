
function hist( id, frequency,  names ) {
var margin = {top: 5, right: 5, bottom: 5, left: 5},
    width = 110 - margin.left - margin.right,
    height = 60 - margin.top - margin.bottom;

var x = d3.scale.ordinal()
    .rangeRoundBands([0, width], .1);

var y = d3.scale.linear()
    .range([height, 0]);

var xAxis = d3.svg.axis()
    .scale(x)
    .orient("bottom");

var yAxis = d3.svg.axis()
    .scale(y)
    .orient("left")
    .ticks(4, "")
    .tickFormat("");

var svg = d3.select(id).append("svg")
    .attr("width", width + margin.left + margin.right)
    .attr("height", height + margin.top + margin.bottom)
  .append("g")
    .attr("transform", "translate(" + margin.left + "," + margin.top + ")");

/*    
    svg.on("mouseover", function(d) {
            d3.select(this).transition()
               .duration(1000)
               .attr("width", 500);
        })
        .on("mouseout", function(d) {
            d3.select(this).transition()
               .duration(1000)
               .attr("width", width + margin.left + margin.right);
        });
*/

  var index = d3.range(frequency.length);
  
  x.domain(names);
  y.domain([0.0,d3.max(frequency)]);
  
  svg.append("g")
      .attr("class", "x axis")
      .attr("transform", "translate(0," + height + ")")
      .call(xAxis);

  svg.append("g")
      .attr("class", "y axis")
      .call(yAxis);
      /*
    .append("text")
      .attr("transform", "rotate(-90)")
      .attr("y", 6)
      .attr("dy", ".71em")
      .style("text-anchor", "end")
      .text("Frequency");
      */

  svg.selectAll(".bar")
      .data(index)
    .enter().append("rect")
      .attr("class", "bar")
      .attr("x", function(d) { return x(names[d]); })
      .attr("width", x.rangeBand() )
      .attr("y", function(d) { return y(frequency[d]); })
      .attr("height", function(d) { return height -  y(frequency[d]); })
      .append("svg:title").text( function(d) { return names[d] + " ("+frequency[d]+")"; } );
}

function toggle(id) {
	   var e = d3.select("#"+id);
	   if(e.style( "display") == 'none') {
	      e.style( "display", "" );
	   }
	   else {
	      e.style( "display", "none" );
	   }
}

function setIframeHeight(target) {
    d3.select('#'+target).select('iframe').style( 'height', d3.select('#'+target).style( 'height' ));
}
