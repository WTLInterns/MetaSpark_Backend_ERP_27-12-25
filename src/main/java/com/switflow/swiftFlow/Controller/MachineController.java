package com.switflow.swiftFlow.Controller;


import org.springframework.web.bind.annotation.RestController;

import com.switflow.swiftFlow.Request.MachinesRequest;
import com.switflow.swiftFlow.Response.MachinesResponse;
import com.switflow.swiftFlow.Service.MachinesService;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;


@RestController
@RequestMapping("/machine")
public class MachineController {
    
    @Autowired
    private MachinesService machinesService;


    @PostMapping("/add-machine")
    @PreAuthorize("hasRole('ADMIN')")
    public MachinesResponse addMachine(@RequestBody MachinesRequest machinesRequest) {
        return machinesService.addMachines(machinesRequest);
    }

    @GetMapping("/getAllMachines")
    public List<MachinesResponse> getAllMachines() {
        return machinesService.getAllMachines();
    }

    @GetMapping("/getMachine/{id}")
    public MachinesResponse getMachine(@PathVariable int id) {
        return machinesService.getMachines(id);
    }

    @PutMapping("/update-machine/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public MachinesResponse updateMachine(@PathVariable int id, @RequestBody MachinesRequest machinesRequest) {
        return machinesService.updateMachines(id, machinesRequest);
    }

    @DeleteMapping("/delete-machine/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteMachine(@PathVariable int id) {
        machinesService.deleteMachines(id);
    }


}
