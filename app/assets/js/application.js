$(document).ready(function () {
  var validator = $('#proxy-form').validate({
    rules: {
      email: {
        required: true,
        email: true
      },
      proxy: {
        required: true,
        minlength: 3,
        remote: "/validate"
      }
    },
    messages: {
      email: {
        required: "Please enter a valid email address",
        email: "Please enter a valid email address"
      },
      proxy: {
        required: "Choose an address",
        minlength: jQuery.format("Enter at least {0} characters"),
        remote: jQuery.format("'{0}@forward.cat' is not valid or already in use")
      }
    },
    errorPlacement: function (error, element) {
      error.appendTo(element.parent().find(".help-block"));
    },
    submitHandler: function (form) {
      $(form).ajaxSubmit({
        url:"/add",
        type:"get",
        success: function() {
          window.location.href = "/email_sent";
        }
      });
    },
    success: function (label) {
      label.remove();
    }
  });

  $("#proxy-form i[rel='tooltip']").popover({
    placement: "top",
    trigger: "manual"
  }).click(function() {
    $(this).popover('show');
    return false; // Stop propagation & prevent default
  });

  $('body').click(function() {
    $('#proxy-form i[rel="tooltip"]').popover('hide');
  });
});
