
$("#myForm").submit(function(e) {

  e.preventDefault();

  console.log("filled");

  $.ajax({
    async: true,
    crossDomain: true,
    url: "app.php",
    method: "POST",
    processData: false,
    data: $("#myForm").serialize(),
    success: function (data) {
      console.log(data);
      var json = JSON.parse(data);
      if(json.success == true) {
        $(".result").html("Status: " +  json.message);
      }
      else {
        $(".result").html("Nastala chyba: " +  json.message);
      }
    },
    error: function(request, static, error) {
      console.log(request);
      $(".result").html("Nastala chyba: " +  request.responseText);
    }
  });
});